package com.github.dapeng.soa;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.soa.service.PrintService;
/**
 * Created by admin on 2017/8/16.
 */

public class PrintServiceImpl implements PrintService {

    @Override
    public String printInfo(com.github.dapeng.soa.info.Info info) throws SoaException {

//        StaffServiceClient staffServiceClient = new StaffServiceClient();
//        TStaffEx staffEx=staffServiceClient.getById(16218);
//        return "say:"+info.code+" staff:"+staffEx.getName();
        return "say:"+info.code+" methodName : printInfo";

    }

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
}
