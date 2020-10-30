package com.succez.filesystem;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Author: yuanshaok
 * @Date: 2020-09-11 11:10:36
 */
public class Request {
	private static Logger log = LogManager.getLogger(Request.class.getName());
	private InputStream input;
	private String url;
	private String method;
	private int contentLength;
	private Map<String, String> requetParam;

	public Request(InputStream input) throws IOException {
		this.input = input;
		parse();
	}

	/**
	 * 读取请求的内容，从请求中获取uri,并设置this.uri = 获取的uri
	 * 
	 * @throws IOException
	 */
	public void parse() throws IOException {
		// 转换成字符流来读第一行,获取第一行的请求类型 GET/POST 和 请求的URL
		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
		// 这里头部信息的作用 1.获取url 2.post请求得到content-length,只读取content-length长度数据后,就停止读取避免一直阻塞
		Map<String, String> head = new HashMap<>();
		String line = null;
		log.debug("准备读取http报文");
		line = bufferedReader.readLine();
		log.debug("http报文读取完毕" + line);
		String[] headMessage = line.split(" ");
		this.method = headMessage[0];
		log.info("请求方法:" + method.toUpperCase());
		// get请求参数在url中,POST请求在body中
		parseUrl(headMessage[1]);
		while (!(line = bufferedReader.readLine()).equals("")) {
			head.put(line.split(":")[0], line.split(":")[1].trim());
		}
		log.info("头部数据读取完毕,完整头部信息为:" + head.toString());
	}

	/**
	 * 得到请求中的参数,可以将参数存储到map中
	 */
	private void parseUrl(String uri) throws UnsupportedEncodingException {
		/**
		 * 这里是因为talend api 自动加入了javascript encodeURIComponent 方法对特殊字符进行了编码,这里需要再一次的解码
		 */
		uri = URLDecoder.decode(uri, "UTF-8");
		log.info("解码后的uri: " + uri);
		if (uri == null) {
			return;
		}
		uri = uri.trim();
		if (uri.equals("")) {
			return;
		}
		String[] uriParts = uri.split("\\?");
		this.url = uriParts[0];
		// 没有参数
		if (uriParts.length == 1) {
			log.info("无请求参数");
			return;
		} else {// 有参数
			String[] params = uriParts[1].split("&");
			requetParam = new HashMap<>();
			for (String param : params) {
				String[] keyValue = param.split("=");
				requetParam.put(keyValue[0], keyValue[1]);
				log.info("请求参数: " + keyValue[0] + "=" + keyValue[1]);
			}
		}

	}

	public String getUrl() {
		return url;
	}

	public InputStream getInput() {
		return input;
	}

	public String getMethod() {
		return method;
	}

	public int getContentLength() {
		return contentLength;
	}

	public Map<String, String> getRequetParam() {
		return requetParam;
	}
}
