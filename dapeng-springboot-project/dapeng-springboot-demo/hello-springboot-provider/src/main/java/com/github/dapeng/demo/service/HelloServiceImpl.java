package com.github.dapeng.demo.service;

import com.dapeng.example.hello.domain.Hello;
import com.dapeng.example.hello.service.HelloService;
import com.github.dapeng.config.annotation.DapengService;
import com.github.dapeng.core.SoaException;

@DapengService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) throws SoaException {
        return "hello : " + name;
    }

    @Override
    public String sayHello2(Hello hello) throws SoaException {
        return "hello : " + hello.name + "-> " + hello.message;
    }
}
