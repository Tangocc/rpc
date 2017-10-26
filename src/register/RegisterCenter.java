package register;

import services.bean.ServiceInfo;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 服务注册中心
 * * 服务提供方注册服务消息
 * * 消费方拉取注册的服务信息
 */
public class RegisterCenter {


    private static final String REGISTER_IP = "127.0.0.1";
    //注册服务监听端口
    private static final int REGISTER_PORT = 4080;
    //拉取服务信息监听端口
    private static final int PULL_PORT = 4081;
    //注册服务处理线程
    private static Executor registerExecutor = Executors.newFixedThreadPool(10);
    //拉取服务信息处理线程
    private static Executor pullExecutor = Executors.newFixedThreadPool(10);
    //注册服务信息
    private static final Map<String, ServiceInfo> serviceMaps = new ConcurrentHashMap<>();

    public void init() {

        System.out.println("----------注册中心启动---------");

        //开启服务端注册服务监听线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("------------注册服务监听线程启动----------------");
                while (true) {
                    ServerSocket socket = null;
                    try {

                        socket = new ServerSocket();
                        socket.bind(new InetSocketAddress(REGISTER_IP, REGISTER_PORT));
                        while (true) {
                            registerExecutor.execute(new RegisterServiceTask(socket.accept()));
                        }
                    } catch (Exception e) {
                        System.out.println("RegisterCenter：" + e.getMessage());
                    }

                }

            }
        }).start();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //主线程开启消费端拉取服务信息监听
                System.out.println("------------客户端服务请求监听线程启动----------------");
                ServerSocket socket = null;
                try {

                    socket = new ServerSocket();
                    socket.bind(new InetSocketAddress(REGISTER_IP, PULL_PORT));
                    while (true) {
                        pullExecutor.execute(new PullServiceTask(socket.accept()));
                    }
                } catch (Exception e) {
                    System.out.println("RegisterCenter：" + e.getMessage());
                } finally {
                    System.out.println("-------------注册中心停止-----------------");
                }
            }
        }).start();

    }


    //向消费端推送服务注册信息
    private static class PullServiceTask implements Runnable {

        private Socket socket;

        PullServiceTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("---------消费端服务获取线程启动------------");
            ObjectInputStream objectInputStream = null;
            ObjectOutputStream objectOutputStream = null;

            try {
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

                String serviceName = (String) objectInputStream.readObject();
                if (!serviceMaps.containsKey(serviceName)) {
                    objectOutputStream.writeObject(null);
                    return;
                }
                ServiceInfo serviceInfo = serviceMaps.get(serviceName);
                objectOutputStream.writeObject(serviceInfo);
            } catch (Exception e) {
                System.out.println("PullServiceTask：" + e.getMessage());
            } finally {

                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }
                    if (objectOutputStream != null) {
                        objectOutputStream.close();
                    }
                } catch (Exception e) {
                    System.out.println("PullServiceTask：" + e.getMessage());
                }

            }
            System.out.println("---------消费端拉取服务信息线程停止------------");
        }
    }

    //服务提供方注册服务
    private static class RegisterServiceTask implements Runnable {

        private Socket socket;

        RegisterServiceTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("---------服务端注册服务线程启动------------");
            ObjectInputStream objectInputStream = null;
            ObjectOutputStream objectOutputStream = null;

            try {
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                ServiceInfo serviceInfo = (ServiceInfo) objectInputStream.readObject();
                String serviceName = serviceInfo.getName();
                serviceMaps.put(serviceName, serviceInfo);
                System.out.println("注册服务：" + serviceInfo.toString());
            } catch (Exception e) {
                System.out.println("RegisterServiceTask：" + e.getMessage());
            } finally {

                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }
                    if (objectOutputStream != null) {
                        objectOutputStream.close();
                    }
                } catch (Exception e) {
                    System.out.println("RegisterServiceTask：" + e.getMessage());
                }

            }
            System.out.println("---------服务端注册服务信息线程停止------------");
        }
    }

}
