/**   
 * @Title: GetExitAccessToken.java 
 * @Package cn.com.school.view.wechat 
 * @Description: TODO
 * @author ruiqing_wang   
 * @date 2015-4-29 下午2:12:37 
 * @version V1.0   
 */
package com.ekz.wechat.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @ClassName: GetExitAccessToken
 * @Description: TODO
 * @author ruiqing_wang
 * @date 2015-4-29 下午2:12:37
 * 
 */
public class GetExistAccessToken {
	
	private static final transient Logger log = LoggerFactory.getLogger(GetExistAccessToken.class);
	
	private static Map<String,String> accessTokenMap = new HashMap<String,String>();  //存放第三方登录key

	// 定义一个私有的静态全局变量来保存该类的唯一实例

	private static GetExistAccessToken getExistAccessToken;

	// / 构造函数必须是私有的

	// / 这样在外部便无法使用 new 来创建该类的实例

	private GetExistAccessToken(){

	}
	// / 定义一个全局访问点
	// / 设置为静态方法
	// / 则在类的外部便无需实例化就可以调用该方法
	public static GetExistAccessToken getInstance(){
		// 这里可以保证只实例化一次
		// 即在第一次调用时实例化
		// 以后调用便不会再实例化
		if (getExistAccessToken == null){
			getExistAccessToken = new GetExistAccessToken();
		}
		return getExistAccessToken;
	}

	@SuppressWarnings("static-access")
	public static String getExistAccessToken()throws Exception {
		String accessToken = accessTokenMap.get("accesstoken");
		log.info("内存中获取到的token值：" + accessToken);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(null == accessToken){
			WechatUtil util = new WechatUtil();
			AccessToken token = util.getAccessToken();
			String accessResult = token.getToken() + "," + sdf.format(new Date(new Date().getTime() + 7000000));
			accessTokenMap.put("accesstoken", accessResult);
			return token.getToken();
		}else{
			String[] accesstokens = accessToken.split(",");
			String accessResult = accesstokens[0];
			String lastTime = accesstokens[1];
			Date now = new Date();
			Date accessExpires = sdf.parse(lastTime);
			if (now.getTime() > accessExpires.getTime()){
				log.info("需要进行更换token值,原有时间为：" + accessExpires);
				WechatUtil util = new WechatUtil();
				AccessToken token = util.getAccessToken();
				accessExpires = new Date(now.getTime() + 7000000);
				String nextTime = sdf.format(accessExpires);
				accessResult = token.getToken();
				accessToken = token.getToken() + "," + nextTime;
				accessTokenMap.remove("accesstoken");
				accessTokenMap.put("accesstoken", accessToken);
			}
			return accessResult;
		}
	}
	
	@SuppressWarnings("static-access")
	public static void refreshToken()throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		WechatUtil util = new WechatUtil();
		AccessToken token = util.getAccessToken();
		String nextTime = sdf.format(new Date(now.getTime() + 7000000));
		String accessToken = token.getToken() + "," + nextTime;
		accessTokenMap.remove("accesstoken");
		accessTokenMap.put("accesstoken", accessToken);
	}
}
