package com.github.dapeng.spring.annotation;

import com.github.dapeng.spring.DapengComponentScanRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * annotate a class as Dapeng Service, which is
 * 1. scanable via @Service
 * 2. automate create a SoaProcessorFactory bean which will be used by dapeng-container
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Service
@Import(DapengComponentScanRegistrar.class)
public @interface DapengService {
}
