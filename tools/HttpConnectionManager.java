package demo;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

@SuppressWarnings("unused")
public class HttpConnectionManager {

    /***** HTTP最大连接数 *****/
    private static final int MAX_TOTAL_CONNECTIONS = 100;
    /***** HTTP最大路由数 *****/
    private static final int MAX_ROUTE_CONNECTIONS = 20;
    /***** HTTP连接超时时间 *****/
    public static final int CONNECT_TIMEOUT = 10000;
    /***** HTTP套接字SOCKET超时时间 *****/
    public static final int SOCKET_TIMEOUT = 15000;

    /***** 连接池管理对象 *****/
    private static volatile PoolingHttpClientConnectionManager connectionManager = null;
    /***** 连接重试  *****/
    private static HttpRequestRetryHandler retryHandler = null;

    /***** 声明对https连接支持 *****/
    private static LayeredConnectionSocketFactory sslsf = null;

    static {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有连接
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            retryHandler = createRetryHandler();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化连接池
     */
    private static void initPool() {
        try {
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", sslsf).register("http", PlainConnectionSocketFactory.INSTANCE).build();
            connectionManager = new PoolingHttpClientConnectionManager(registry);
            // MaxTotal连接池最大并发连接数
            connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
            /*
             * DefaultMaxPerRout表示单路由的最大并发连接数，
             * 假设你的业务系统需要调用A和B这两个外部系统的http接口，那么如果DefaultMaxPerRout=100，
             * 那么调用A系统的http接口时，最大并发数就是100
             */
            connectionManager.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建请求重试规则
     * @return
     */
    private static HttpRequestRetryHandler createRetryHandler() {
        HttpRequestRetryHandler retryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException ex, int retryCount, HttpContext context) {
                System.out.println(ex.getClass().getName());
                if (retryCount >= 3) { // 重试3次后放弃
                    return false;
                }
                if (ex instanceof NoHttpResponseException) { // 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (ex instanceof SSLHandshakeException) { // 不要重试SSL握手异常
                    return false;
                }
                if (ex instanceof InterruptedIOException) { // 超时
                    return false;
                }
                if (ex instanceof UnknownHostException) { // 目标服务器不可达
                    return false;
                }
                if (ex instanceof ConnectTimeoutException) { // 连接被拒绝
                    return false;
                }
                if (ex instanceof SSLException) {// ssl握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
        return retryHandler;
    }

    /**
     * 从连接池获取连接
     * @param connectTimeout HTTP连接超时时间 
     * @param socketTimeout HTTP套接字SOCKET超时时间，超过此时间不再读取响应
     * @return 连接对象
     */
    private static CloseableHttpClient getHttpClientFromPool(int connectTimeout, int socketTimeout) {
        if (connectionManager == null) {
            synchronized (HttpConnectionManager.class) {
                if (connectionManager == null) {
                    initPool();
                }
            }
        }
        if (connectTimeout == 0) {
            connectTimeout = CONNECT_TIMEOUT;
        }
        if (socketTimeout == 0) {
            socketTimeout = SOCKET_TIMEOUT;
        }
        /*
         * ConnectTimeout： 链接建立的超时时间； SocketTimeout：响应超时时间，超过此时间不再读取响应；
         * ConnectionRequestTimeout： 从连接池中获得一个连接的超时时间
         */
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).build();

        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig).setRetryHandler(retryHandler).build();
        return httpClient;
    }

    /**
     * 创建新连接
     * @param connectTimeout HTTP连接超时时间 
     * @param socketTimeout HTTP套接字SOCKET超时时间，超过此时间不再读取响应
     * @return 连接对象
     */
    private static CloseableHttpClient createHttpClient(int connectTimeout, int socketTimeout) {
        if (connectTimeout == 0) {
            connectTimeout = CONNECT_TIMEOUT;
        }
        if (socketTimeout == 0) {
            socketTimeout = SOCKET_TIMEOUT;
        }
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).build();
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(sslsf).setRetryHandler(retryHandler).build();
        return httpClient;
    }

    /**
     * 获取连接
     * @return
     */
    public static CloseableHttpClient getHttpClient() {
        return createHttpClient(0, 0);
    }
}