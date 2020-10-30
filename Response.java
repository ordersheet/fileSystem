package com.succez.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.succez.bean.FileMessage;
import com.succez.practice.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * http响应类，用来响应http请求，最后将文件包装成http报文格式返回给浏览器
 *
 * @Author: yuanshaok
 * @Date: 2020-09-11 11:34:27
 */
public class Response {
    private static Logger log = LogManager.getLogger(Response.class.getName());
    private BufferedOutputStream bos;
    private Request request;

    public Response(OutputStream output, Request request) {
        this.bos = new BufferedOutputStream(output);
        this.request = request;
    }
    private static final Map<String, Method> dispather = new ConcurrentHashMap<>();

    /**
     * 这里将请求的静态资源写入outputstream中，错误则显示错误页面
     */
    public void sendStaticResource() throws IOException {
        // URL为"/"已经在这里处理
        if (request.getUrl().equals("/")) {
            // 去找到默认页面输出 下次修改(用相对路径进行输出)
            File file = new File(HttpServer.WEB_ROOT + "/html/file.html");
            log.info("请求默认文件：" + file.getAbsolutePath());
            String fileType = FileUtils.getFileTypeBySuffix(file.getName());
            log.info("请求文件类型: " + fileType);
            byte[] bytes = FileUtils.convertFileToByteArray(file);
            String result = Results.succes(200, "OK", fileType, bytes.length);
            log.info("请求结果:" + result);
            bos.write(result.getBytes());
            bos.write(bytes);
            bos.flush();
            return;
        }

        // 这里可能会报空指针异常
        // 检验两个东西 1.url 2.参数
        // 情况1 有url无参
        String path = "D:\\";
        Map<String, String> requetParam = request.getRequetParam();
        if (request.getUrl() != null && requetParam == null) {
            // 请求静态文件
            path = HttpServer.WEB_ROOT + request.getUrl();
            log.info("请求的静态文件:" + path);
        }
        // 情况2 有url有参
        if (request.getUrl() != null && requetParam != null) {
            if (requetParam.containsKey("path")) {
                path = requetParam.get("path");
            }
        }
        // 如果不存在这个值，那么就将path填充进去
        File file = new File(path);
        // 这里判断文件是否存在
        if (!file.exists()) {
            log.info("请求的文件不存在");
            bos.write(Results.fail(404, "File Not Found").getBytes());
            bos.flush();
            return;
        }
        if (file.isDirectory()) {// 处理为文件夹的内容
            dirctoryHandler(path);
        } else {// 这里处理请求的为文件内容
            fileHandler(file);
        }
    }

    /**
     * 返回文件
     * 
     * @param file
     * @throws IOException
     */
    private void fileHandler(File file) throws IOException {
        // 判断文件类型,先放在前面，最好是在上传前就确定文件类型
        String fileType = FileUtils.getFileTypeBySuffix(file.getAbsolutePath());
        /**
         * 这里先读成字节数组,取出字节数组的长度作为content-length传入http报文中,最后将字节写入输出缓冲流中
         * 中间需要有转为字节数组的过程,BufferedInputStream.available()获取不到具体的长度
         */
        byte[] bytes = FileUtils.convertFileToByteArray(file);
        String result = Results.succes(200, "OK", fileType, bytes.length);
        log.info("响应头信息:" + result);
        bos.write(result.getBytes());
        bos.write(bytes);
        bos.flush();
    }

    /**
     * 处理文件夹内容,将文件夹的数据列出 以json方式返回到页面上 返回的数据有文件名称,文件大小(字节),文件修改时间
     * example:(这里只给出了一个文件的示例,多个文件以json数组形式返回) {"date":"2020年7月31日
     * 下午5:54:19","fileName":"$RECYCLE.BIN","size":0}
     * 
     * @param path 文件夹路径
     * @throws IOException
     */
    private void dirctoryHandler(String path) throws IOException {
        log.info("请求的文件夹路径:" + path);
        List<FileMessage> list = FileUtils.showFiles(path);
        String result = Results.succes(200, "OK", list);
        log.info("响应结果" + result);
        bos.write(result.getBytes());
        bos.flush();
    }
}