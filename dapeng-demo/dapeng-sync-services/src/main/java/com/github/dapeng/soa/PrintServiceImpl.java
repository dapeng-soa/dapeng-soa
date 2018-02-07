package com.github.dapeng.soa;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.soa.domain.Info;
import com.github.dapeng.soa.service.PrintService;
/**
 * Created by admin on 2017/8/16.
 */

public class PrintServiceImpl implements PrintService {

    @Override
    public String printInfo2(String name){

        System.out.println("Receiver String Message : " + name);
        return "hello,"+name;

    }

    @Override
    public String printInfo3() throws SoaException {
        return null;
    }

    @Override
    public void print(){
        System.out.println("test void method");
    }

    @Override
    public String printInfo(Info info) throws SoaException {
        return "say:"+info.code+" methodName : printInfo";
    }
}
