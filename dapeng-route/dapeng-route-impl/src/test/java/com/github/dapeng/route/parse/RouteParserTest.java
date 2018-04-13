package com.github.dapeng.route.parse;

import com.github.dapeng.route.Route;
import com.github.dapeng.route.RouteExecutor;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.route.Route;
import com.github.dapeng.route.RouteExecutor;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Eric on 2016-06-21
 * @date
 */
public class RouteParserTest {

    public static void main(String args[]) {
        String source = "      operatorId match ~%'1024n+0..9' => ~ip'1.2.3.4'\n" +
                "      operatorId match n'10|11|12|13' => ~ip'1.2.3.4'\n" +
                "      operatorId match ~n'10..20' => ~ip'1.2.3.4'\n" +
                "      callerMid match s'app|oss' => ~ip'1.2.3/24'\n" +
                "      ip match ~ip'1.2.3.0/24|192.168.3.39' => ~ip'1.2.3.4|192.168.1.39/32'\n";

//        String str = "operatorId match %'1024n+0..9' and ip match ip'192.168.3/24' => ~ip'1.2.3.4|192.168.1.1/24'";
//        String str = "service match s'ArticleService' => ~ip'1.2.3.4|192.168.1.1/24'";
        String str = "service match s'com.github.dapeng.soa.user.service.UserService' and version match s'1.0.0' => ~ip'192.168.3.39'";
        RouteParser parser = new RouteParser();

        List routes = new ArrayList<Route>();
        parser.parseAll(routes, str);

        List<String> servers = new ArrayList<>();
        servers.add("192.168.3.38");
        servers.add("192.168.3.39");
        servers.add("192.168.3.40");

        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

        ctx.operatorId(1024L);
        ctx.userIp("192.168.3.39");
        ctx.callerMid("app");

        ctx.serviceName("com.github.dapeng.soa.user.service.UserService");
        ctx.versionName("1.0.0");
        ctx.methodName("getArticleDetail");

        Set<InetAddress> serverResult = RouteExecutor.execute(ctx, routes, servers);

        System.out.println(serverResult);

    }
}
