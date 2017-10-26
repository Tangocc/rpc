package server;

import services.bean.ServiceInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RpcServer {

    private static final String REGISTER_CENTER_IP = "127.0.0.1";

    private static final int REGISTER_CENTER_PORT = 4080;

    private static Executor serviceExecutor = Executors.newFixedThreadPool(10);

    //注册中心地址
    private static SocketAddress socketAddress = new InetSocketAddress(REGISTER_CENTER_IP, REGISTER_CENTER_PORT);

    //注册服务
    private void registerService(String ip, int port, String serviceName) {

        //连接注册中心
        Socket socket = new Socket();
        ObjectInputStream objectInputStream = null;
        ObjectOutputStream objectOutputStream = null;

        try {
            socket.connect(socketAddress);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setIp(ip);
            serviceInfo.setName(serviceName);
            serviceInfo.setPort(port);
            objectOutputStream.writeObject(serviceInfo);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {

            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
                if (objectInputStream != null) {
                    objectInputStream.close();
                }

            } catch (IOException e) {
                System.out.println("RpcServer: " + e.getMessage());
            }

        }

    }

    public void publishService(String ip, int port, String name) {

        //注册中心注册服务
        registerService(ip, port, name);

        //创建服务监听服务端口
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("----------服务端提供服务-----------");
                InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", port);

                try {
                    ServerSocket socket = new ServerSocket();
                    socket.bind(socketAddress);

                    while (true) {
                        serviceExecutor.execute(new ServiceTask(socket.accept()));
                    }

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

    }

    //执行service 并将结果返回
    private class ServiceTask implements Runnable {
        private Socket socket;

        ServiceTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("-----------本地执行服务，并返回结果------------");
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                String serviceName = (String) objectInputStream.readObject();
                String methodName = (String) objectInputStream.readObject();
                Class<?>[] paramType = (Class<?>[]) objectInputStream.readObject();
                Object[] args = (Object[]) objectInputStream.readObject();
                System.out.println(serviceName + " " + methodName + " " + paramType);
                Class service = Class.forName(serviceName);
                Object obj = service.newInstance();
                Method method = service.getDeclaredMethod(methodName, paramType);
                Object resObject = method.invoke(obj, args);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("执行结果：" + resObject);
                objectOutputStream.writeObject(resObject);

            } catch (Exception e) {
                System.out.println("ServiceTask:" + e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.out.println("ServiceTask:" + e.getMessage());
                }
            }

        }
    }
}
