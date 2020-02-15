package com.github.dapeng.demo;

import com.github.dapeng.config.spring.context.annotation.DapengComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.openfeign.EnableFeignClients;

@DapengComponentScan
@EnableFeignClients({"com.github.dapeng.api"})
@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
public class DemoAnnotationApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DemoAnnotationApplication.class);
    }

    public static void main(String[] args) {
        try {
            SpringApplication.run(DemoAnnotationApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
