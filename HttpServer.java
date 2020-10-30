package com.succez.filesystem;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * web服务器主类，解析请求和将请求分配给response进行响应 这里每次有请求就会创建一个request和response
 * 中文编码问题为解决，代码运行效率不高，未完成get/post请求区别 响应格式不清晰方便 待优化
 * 
 * @Author: yuanshaok
 * @Date: 2020-09-11 11:22:20
 */
public class HttpServer {

    private static int port = 8080;
    public static final String SHUTDOWN_COMMAND = "/SHUTDOWN";
    public static final String WEB_ROOT = "web\\static-files";
    private static Logger log = LogManager.getLogger(HttpServer.class.getName());
    private static ExecutorService executor = new ThreadPoolExecutor(6, 10, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(10));

    static {
        Properties prop = new Properties();
        if (prop.getProperty("server.port") != null) {
            String serverPort = prop.getProperty("server.port");
            try {
                port = Integer.parseInt(serverPort);
                log.debug("端口初始化····" + port);
            } catch (NumberFormatException e) {
                log.error("解析config.properties失败,出错的参数 port" + serverPort);
                throw new RuntimeException("port参数出错,仅支持数字");
            }
        }

    }

    /**
     * 初始化一个contentTyep,用于在浏览器显示文件时将content=type自动转换为文件对应的类型
     */
    public static void main(String[] args) {
        final HttpServer server = new HttpServer();
        log.info("容器启动");
        try {
            server.startService();
        } catch (IOException e) {
            log.error("容器启动失败" + e);
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * 这里是使用ServerSocket来获取来自客户端的socket,端口号为8080 队列长度为10
     * 执行容器的作用，获取客户端请求，每次客户端的请求都会包装进request{@link Request}
     * 
     * @throws IOException
     */
    private void startService() throws IOException {
        ServerSocket serverSocket = null;
        log.debug("等待请求");
        serverSocket = new ServerSocket(port, 10);
        /**
         * 循环等待客户端请求，并将请求包装进request{@link Request}和response{@ Response}
         */
        while (true) {
            Socket socket = serverSocket.accept();
            log.debug("收到请求");
            executor.execute(() -> {
                try {
                    final InputStream input = socket.getInputStream();
                    final OutputStream output = socket.getOutputStream();
                    // 新建request对象，解析出uri
                    Request request = new Request(input);
                    if (request.getUrl() != null && request.getUrl().equals(SHUTDOWN_COMMAND)) {
                        log.info("收到SHUTDOWN信息容器正常退出");
                        System.exit(1);
                    }
                    // 把输出流传入response,由response进行写入
                    log.debug("准备处理请求");
                    final Response response = new Response(output, request);
                    response.sendStaticResource();
                    // 关闭流会导致socket关闭，所以不手动关闭流，等待socket关闭。
                    log.info("socket关闭请求结束");
                    socket.close();
                } catch (IOException e) {
                    log.error("IO异常" + e);
                    e.printStackTrace();
                }
            });
        }

    }

}
