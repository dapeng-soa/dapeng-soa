package com.github.dapeng.demo.controller;


import com.dapeng.example.hello.service.HelloService;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.demo.service.HelloServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    @Autowired
    private HelloServiceImpl helloService;

    @RequestMapping("/hello")
    public String hello(String name) throws SoaException {
        return helloService.sayHello(name);

    }
}
