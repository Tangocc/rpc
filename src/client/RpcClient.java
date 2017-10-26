package client;

import services.bean.ServiceInfo;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class RpcClient<T> {

    private static final String REGISTER_IP = "127.0.0.1";
    private static final int REGISTER_PORT = 4081;
    private static final SocketAddress REGISTER_SOCKETADDRESS = new InetSocketAddress(REGISTER_IP, REGISTER_PORT);
    private static final Map<String, ServiceInfo> serviceMaps = new HashMap<>();

    //从注册中心拉去服务信息并缓存
    private void pullService(String serviceName) {

        System.out.println("----------从注册中心拉取服务信息----------");
        Socket socket = new Socket();
        ServiceInfo obj = null;
        ObjectInputStream objectInputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            socket.connect(REGISTER_SOCKETADDRESS);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(serviceName);
            Thread.sleep(10);
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            obj = (ServiceInfo) objectInputStream.readObject();
            if (obj == null) {
                return;
            }
            serviceMaps.put(serviceName, obj);

            System.out.println("----------从注册中心拉取服务信息完成----------");
        } catch (Exception e) {
            System.out.println("RpcClient:" + e.getMessage());
        } finally {

            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (Exception e) {
                System.out.println("RpcClient:" + e.getMessage());
            }


        }


    }

    public Object importer(final Class<?> serviceClass, String service) {

        return Proxy.newProxyInstance(serviceClass.getClassLoader(), serviceClass.getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                Socket socket = null;
                ObjectInputStream objectInputStream = null;
                ObjectOutputStream objectOutputStream = null;

                try {

                    if (!serviceMaps.containsKey(service)) {
                        pullService(service);
                    }

                    if (!serviceMaps.containsKey(service)) {
                        return null;
                    }
                    ServiceInfo serviceInfo = (ServiceInfo) serviceMaps.get(service);
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(serviceInfo.getIp(), serviceInfo.getPort()));

                    if (!socket.isConnected()) {
                        //没有连接  则返回null
                        return null;
                    }

                    objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    objectOutputStream.writeObject(serviceClass.getName());
                    objectOutputStream.writeObject(method.getName());
                    objectOutputStream.writeObject(method.getParameterTypes());
                    objectOutputStream.writeObject(args);

                    objectOutputStream.flush();
                    Thread.sleep(10);
                    objectInputStream = new ObjectInputStream(socket.getInputStream());
                    return objectInputStream.readObject();
                } catch (Exception e) {
                    System.out.println("RpcClient:"+e.getMessage());

                } finally {
                    if (socket != null) {
                        socket.close();
                    }
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }

                }
                return null;
            }
        });
    }


}
