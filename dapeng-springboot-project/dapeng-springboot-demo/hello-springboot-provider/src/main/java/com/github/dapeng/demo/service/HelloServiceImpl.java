package com.github.dapeng.demo.service;

import com.dapeng.example.hello.domain.Hello;
import com.dapeng.example.hello.service.HelloService;
import com.github.dapeng.api.RemoteStoreService;
import com.github.dapeng.api.dto.StoreRequest;
import com.github.dapeng.api.dto.StoreResponse;
import com.github.dapeng.config.annotation.DapengService;
import com.github.dapeng.core.SoaException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

@DapengService
public class HelloServiceImpl implements HelloService {

    @Autowired
    private RemoteStoreService storeService;

    @Override
    public String sayHello(String name) throws SoaException {
        StoreRequest storeRequest = new StoreRequest();
        storeRequest.setNumberId(123);
        storeRequest.setName(name);
        storeRequest.setAttachment(new HashMap<>());

        StoreResponse response = storeService.store(storeRequest);

        return "hello : " + name + "resp: " + response.toString();
    }

    @Override
    public String sayHello2(Hello hello) throws SoaException {
        return "hello : " + hello.name + "-> " + hello.message;
    }
}
