package demo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpUtil {

    /**
     * 使用Get方式获取数据
     * @param url url
     * @param paramMap 参数（如果url中已包含参数则可以为null）
     * @param header 消息头（可以为null）
     * @return
     */
    public static String sendGet(String url, Map<String, String> paramMap, Map<String, String> header) {

        String result = "";
        
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            if (null != paramMap && !paramMap.isEmpty()) {
                List<NameValuePair> list = new ArrayList<>(paramMap.size());
                for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                    list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(list, "UTF-8"));
            }
            client = HttpConnectionManager.getHttpClient();
            HttpGet httpGet = new HttpGet(url);

            // 设置消息头
            if (null != header && !header.isEmpty()) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpGet.addHeader(entry.getKey(), entry.getValue());
                }
            }

            response = client.execute(httpGet);
            result = getResult(response);
            httpGet.releaseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            relase(response);
        }
        return result;
    }

    /**
     * POST请求，json形式数据
     * @param url 请求地址
     * @param param 请求数据json格式
     * @param header 消息头 没有可为null
     */
    public static String sendPostJson(String url, String param, Map<String, String> header) {

        String result = "";

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpConnectionManager.getHttpClient();
            HttpPost httpPost = new HttpPost(url);

            // 设置消息头
            if (null != header && !header.isEmpty()) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
            }

            // 设置参数
            StringEntity stringEntity = new StringEntity(param, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);

            response = client.execute(httpPost);
            result = getResult(response);
            httpPost.releaseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            relase(response);
        }

        return result;
    }

    /**
     * POST请求，Map形式数据
     * @param url 请求地址
     * @param param 请求数据
     * @param charset 编码方式
     */
    public static String sendPost(String url, Map<String, String> param, Map<String, String> header) {

        String result = "";

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = HttpConnectionManager.getHttpClient();
            HttpPost httpPost = new HttpPost(url);

            // 设置消息头
            if (null != header && !header.isEmpty()) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    httpPost.addHeader(entry.getKey(), entry.getValue());
                }
            }

            // 设置参数
            if (null != param && !param.isEmpty()) {
                List<NameValuePair> list = new ArrayList<>();
                for (Map.Entry<String, String> entry : param.entrySet()) {
                    NameValuePair valuePair = new BasicNameValuePair(entry.getKey(), entry.getValue());
                    list.add(valuePair);
                }
                if (!list.isEmpty()) {
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
                    httpPost.setEntity(entity);
                }
            }

            response = client.execute(httpPost);
            result = getResult(response);
            httpPost.releaseConnection();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            relase(response);
        }
        return result;
    }

    /**
     * 获取结果
     * @param response
     * @return
     * @throws ParseException
     * @throws IOException
     */
    private static String getResult(CloseableHttpResponse response) throws ParseException, IOException {
        String result = "";
        if (response != null) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                Charset charset = ContentType.getOrDefault(httpEntity).getCharset();
                String charsetName = (charset != null) ? charset.name() : "UTF-8";
                result = EntityUtils.toString(httpEntity, charsetName);
            }
        }
        return result;
    }

    /**
     * 释放连接
     * @param response
     */
    private static void relase(CloseableHttpResponse response) {
        if (response != null) {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }
}
