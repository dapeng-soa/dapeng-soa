package com.github.dapeng.service.invoke;

import com.github.dapeng.service.invoke.BaseController;
import com.github.dapeng.service.invoke.entity.BaseRequest;

import java.io.IOException;

/**
 * Created by lihuimin on 2017/12/6.
 */
public class Test {

    public static void main(String[] args) throws IOException {
        System.setProperty("soa.zookeeper.host","192.168.99.100:2181");

        String jsonParam = "{\"id\": 210}";

        BaseRequest request = new BaseRequest(jsonParam,"com.github.dapeng.soa.company.service.CompanyService","1.0.0","findCompanyById");

        BaseController controller = new BaseController();
        String result = controller.invoke(request);

        System.out.println(result);


    }
}
