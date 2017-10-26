package services.impl;

import services.service.HelloWorldService;

public class HelloWorldServiceImpl implements HelloWorldService {

    @Override
    public String say() {
        return "Hello World";
    }
}
