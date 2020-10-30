package com.succez.filesystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson.JSON;

/**
 * 修正错误:message为中文时，一个中文占两个字节，content-length计算错误。可新增判断是否为中文，或者先转为byte数组
 * 
 * @Author: yuanshaok
 * @Date: 2020-09-15 10:56:10
 */
public class Results {
    // 版本 若固定则可以不定义
    private static final String HTTP_VERSION = "HTTP/1.1";
    //
    public final static String CHARSET = "charset=UTF-8";
    /**
     * 状态码和响应的消息,这里存入一些常用的供使用者直接使用
     */
    public static final Map<Integer, String> STATU_CODE;
    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(200, "OK");
        map.put(400, "Bad Request");
        map.put(401, "Unauthonzed");
        map.put(403, "Forbidden");
        map.put(404, "Not Found");
        map.put(500, "Internal Server Error");
        map.put(503, "Service Unavailable");
        STATU_CODE = Collections.unmodifiableMap(map);
    }
    /**
     * 中间层有时会初始化一些final的静态的Map供给一些字段做映射,但是代码检查时会报错： 就是说声明了final
     * static，这个map还是可以修改，所以这里需要一个不可更改的map，
     * Collections.unmodifiableMap()方法会返回一个“只读”的map，当你调用此map的put方法时会抛错
     * 每次请求都需要使用所以作为常量根据文件类型给http响应报文传入content-type,这里并没有写出所有的类型,只是列举出常用的
     */
    public static final Map<String, String> CONTENT_TYPE;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("jpe", "image/jpeg");
        map.put("jpeg", "image/jpeg");
        map.put("jpf", "image/jp2");
        map.put("jpg", "image/jpeg");
        map.put("png", "image/png");
        map.put("js", "application/javascript");
        map.put("mp3", "audio/mpeg");
        map.put("mp4", "video/mp4");
        map.put("ppt", "application/vnd.ms-powerpoint");
        map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        map.put("tar", "application/x-tar");
        map.put("tar.gz", "application/x-compressed-tar");
        map.put("zip", "application/zip");
        map.put("class", "application/x-java");
        map.put("doc", "application/msword");
        map.put("css", "text/css");
        map.put("txt", "text/plain");
        map.put("html", "text/html");
        map.put("htm", "text/html");
        map.put("htx", "text/html");
        map.put("java", "java/*");
        map.put("jsp", "text/html");
        CONTENT_TYPE = Collections.unmodifiableMap(map);
    }

    /**
     * 包装获取数据成功后的http响应头信息,使浏览器可以识别,这里包装较麻烦 若是文件
     * 
     * @param code     状态码
     * @param message  消息
     * @param fileType 文件类型与{@link Results#CONTENT_TYPE}对应
     * @param length   传入报文数据长度 传入Content-Length
     * @return http头信息
     */
    public static String succes(int code, String message, String fileType, int length) {
        StringBuilder sb = new StringBuilder();
        sb.append(HTTP_VERSION + " ");
        sb.append(code + " " + message + "\r\n");
        sb.append("Content-Type: ");
        if (CONTENT_TYPE.get(fileType) == null) {
            sb.append("text/html");
        } else {
            sb.append(CONTENT_TYPE.get(fileType));
        }
        sb.append(";");
        sb.append(CHARSET + "\r\n");
        sb.append("Content-Length: ");
        sb.append(length);
        sb.append("\r\n\r\n");
        return sb.toString();
    }

    /**
     * 一个简单的返回信息包装，这个方法会返回json，用于处理ajax数据请求
     * 
     * @param code    状态码
     * @param message 消息
     * @param data    html文件
     * @return http头部信息+json
     */
    public static String succes(int code, String message, Object data) {
        StringBuilder sb = new StringBuilder();
        String resultData = JSON.toJSONString(data);
        sb.append(HTTP_VERSION + " ");
        sb.append(code + " " + message + "\r\n");
        sb.append("Content-Type: ");
        sb.append("text/html");
        sb.append(";");
        sb.append(CHARSET + "\r\n");
        sb.append("Content-Length: ");
        sb.append(resultData.getBytes().length);
        sb.append("\r\n\r\n");
        sb.append(resultData);
        return sb.toString();
    }

    /**
     * 包装失败后http的响应报文头部信息
     * 
     * @param code    状态码
     * @param message 响应信息
     * @return 头部信息
     */
    public static String fail(int code, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(HTTP_VERSION + " ");
        sb.append(code + " " + message + "\r\n");
        // sb.append("Content-Type: text/html;" + CHARSET + "\r\n");
        sb.append("Content-Type: " + CHARSET + "\r\n");
        // 这里可以给一个错误页面供显示，暂时没写，不给,等后续写html时将html返回给前端显示
        // 9为<h1></h1>的长度
        sb.append("Content-Length: " + (message.getBytes().length + 9));
        sb.append("\r\n\r\n");
        sb.append("<h1>" + message + "</h1>");
        return sb.toString();
    }

    /**
     * 包装失败后的头部信息 这里状态码如果是已经定义的则使用已经定义的,没有则状态码为499 信息为"未知错误"
     * 
     * @param code 状态码
     * @return 头部信息
     */
    public static String fail(int code) {
        StringBuilder sb = new StringBuilder();
        sb.append(HTTP_VERSION + " ");
        if (STATU_CODE.get(code) == null) {
            sb.append("499 未知错误\r\n");
            sb.append("Content-Type: text/html;" + CHARSET + "\r\n");
            sb.append("Content-Length: 20");
            sb.append("\r\n\r\n");
            sb.append("<h1>UnknowError</h1>");
        } else {
            String message = STATU_CODE.get(code);
            sb.append(code + " " + message + "\r\n");
            sb.append("Content-Type: text/html;" + CHARSET + "\r\n");
            sb.append("Content-Length: " + (message.getBytes().length + 9) + "\r\n\r\n");
            sb.append("<h1>" + message + "</h1>");
        }
        return sb.toString();
    }
}
