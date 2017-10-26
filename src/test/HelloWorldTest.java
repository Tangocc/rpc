package test;

import client.RpcClient;
import register.RegisterCenter;
import server.RpcServer;
import services.impl.HelloWorldServiceImpl;
import services.service.HelloWorldService;

public class HelloWorldTest {

    public static void main(String[] args) throws InterruptedException {

        //启动注册中心
        RegisterCenter registerCenter = new RegisterCenter();
        registerCenter.init();
        Thread.sleep(1000);

        //启动服务端，并发布服务
        RpcServer rpcServer = new RpcServer();
        rpcServer.publishService("127.0.0.1",5000,"helloworld.service");

        Thread.sleep(1000);
        RpcClient<HelloWorldService> rpcClient = new RpcClient<>();
        HelloWorldService helloWorldService = (HelloWorldService) rpcClient.importer(HelloWorldServiceImpl.class,"helloworld.service");
        String result =   helloWorldService.say();
        System.out.println(result);

        Thread.sleep(1000);
        System.out.println("---------Test Finished-----------");
    }
}
