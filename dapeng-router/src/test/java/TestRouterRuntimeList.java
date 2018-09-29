import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import com.github.dapeng.router.exception.ParsingException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午10:00
 */
public class TestRouterRuntimeList {


    private RuntimeInstance runtimeInstance1 = new RuntimeInstance("com.maple.Uservice", "192.168.1.101", 9090, "1.0.0");
    private RuntimeInstance runtimeInstance2 = new RuntimeInstance("com.maple.Uservice", "192.168.1.102", 9091, "1.0.0");
    private RuntimeInstance runtimeInstance3 = new RuntimeInstance("com.maple.Uservice", "192.168.1.103", 9092, "1.0.0");
    private RuntimeInstance runtimeInstance4 = new RuntimeInstance("com.maple.Uservice", "192.168.1.104", 9093, "1.0.0");


    public List<RuntimeInstance> prepare(InvocationContextImpl ctx, List<Route> routes) {
        List<RuntimeInstance> instances = new ArrayList<>();

        instances.add(runtimeInstance1);
        instances.add(runtimeInstance2);
        instances.add(runtimeInstance3);
        instances.add(runtimeInstance4);

        List<RuntimeInstance> filterInstances = RoutesExecutor.executeRoutes(ctx, routes, instances);
        return filterInstances;
    }


    @Test
    public void testRouterOneMatch() {
        String pattern = "  method match 'getFoo' , 'setFoo' ; version match '1.0.0' => ip\"192.168.1.101/30\"" +
                System.getProperty("line.separator") + " method match 'getFoo1' , 'setFoo' ; version match '1.0.0' => ip\"192.168.1.101/30\"";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.serviceName("getSkuById");
        ctx.versionName("1.0.0");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);
        expectInstances.add(runtimeInstance4);


        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    /**
     * 测试 匹配 成功 路由多个 ip
     * 如果配置了 not ip ~103 它的优先级最高， 如果再配置 普通规则， 也以非为优先
     * <p>
     * 既存在 ~  又存在 ip ，  先过滤 ~ 再过滤 正常的。 如下结果应该是 102
     */
    @Test
    public void testRouter() {
        String onePattern_oneMatcher = "method match 'getFoo' , 'setFoo' ; version match '1.0.0' => ip'192.168.1.102' , ~ip'192.168.1.101' ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("getFoo");
        ctx.versionName("1.0.0");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance2);


        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 测试 匹配 成功 路由多个 ip
     * 如果配置了 not ip ~103 它的优先级最高， 如果再配置 普通规则， 也以非为优先
     */
    @Test
    public void testRouterOne() {
        String onePattern_oneMatcher = "method match 'getFoo' , 'setFoo' ; version match '1.0.0' => ~ip'192.168.1.103' ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("getFoo");
        ctx.versionName("1.0.0");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance4);


        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 测试 匹配 成功 路由多个 ip  非ip ～
     */
    @Test
    public void testRouterNot() {
        String onePattern_oneMatcher = "method match 'getFoo' , 'setFoo' ; version match '1.0.0' => ~ip'192.168.1.101' , ~ip'192.168.1.103' ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("getFoo");
        ctx.versionName("1.0.0");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance4);


        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 匹配非 规则 ， 一个 not
     */
    @Test
    public void testRouterNotOne() {
        String onePattern_oneMatcher = "method match 'getFoo' , 'setFoo' ; version match '1.0.0' => ~ip'192.168.1.101' ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("getFoo");
        ctx.versionName("1.0.0");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);
        expectInstances.add(runtimeInstance4);


        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testRouterRegex1() {
        String onePattern_oneMatcher = "method match 'getFoo' , r'setFoo.*' ; version match '1.0.0' => ip'192.168.1.101' , ip'192.168.1.103' ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("setFooById");
        ctx.versionName("1.0.0");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());

    }


    /**
     * 正则 匹配 测试
     */
    @Test
    public void testRouterRegex2() {
        String onePattern_oneMatcher = "method match 'getFoo' , r'[a-zA-Z]{3}[0-9]{2}id' ; version match '1.0.0' => ip'192.168.1.101' , ip'192.168.1.103' ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("sat22id");
        ctx.versionName("1.0.0");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());

    }

    @Test
    public void testRouterOneMatch3() {
        String pattern = "  method match r'get.*' , 'setFoo' ; version match '1.0.0' => ~ip\"192.168.1.101\"  " +
                System.getProperty("line.separator") + " otherwise => ip\"192.168.1.101\" , ip\"192.168.1.103\"";

//        String onePattern_oneMatcher = "method match 'get.*ById'  => ip'192.168.1.101/23' , ip'192.168.1.103/24' ";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("getSkuById");
        ctx.versionName("1.0.0");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);
        expectInstances.add(runtimeInstance4);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 取模测试
     */

    @Test
    public void testRouterOneMatch4() {
        String pattern = "  userId match %\"1024n+2..4\"; version match '1.0.0' => ip\"192.168.1.101\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.serviceName("getSkuById");
        ctx.versionName("1.0.0");
        ctx.userId(2052L);
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 取模测试2
     */

    @Test
    public void testRouterOneMatch5() {
        try {

            String pattern = "  userId match %\"1024n+2..4\"; version match '1.0.0' => ip\"192.168.1.101\' ";

            List<Route> routes = RoutesExecutor.parseAll(pattern);
            InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
            ctx.serviceName("getSkuById");
            ctx.versionName("1.0.0");
            ctx.userId(2052L);
            List<RuntimeInstance> prepare = prepare(ctx, routes);

            List<RuntimeInstance> expectInstances = new ArrayList<>();
            expectInstances.add(runtimeInstance1);
        } catch (ParsingException e) {
//            Assert.a(" 抛出了ParsingException 异常 ");
        }

//        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    @Test
    public void testRouterMode2() {
        String pattern = "  userId match %\"1025n+2..4\"; version match '1.0.0' => ip\"192.168.1.101\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.serviceName("getSkuById");
        ctx.versionName("1.0.0");
        ctx.userId(2052L);
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * otherwise 支持，这一条应该放在最后
     */

    @Test
    public void testRouterOtherwise() {
        String pattern = "  otherwise => ~ip\"192.168.1.104/30\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    @Test
    public void testRouterMode() {

        String pattern = "  userId match %'1024n+1..2' => ~ip\"192.168.1.104/30\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

        ctx.userId(1025L);
//        ctx.userId(1026L);
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testRouterMode1() {

        String pattern = "  userId match %'1024n+3' => ~ip\"192.168.1.104/30\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

        ctx.userId(1027L);
        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testRouterRanger() {

        String pattern = "  userId match 10..30 => ~ip\"192.168.1.104/30\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

        ctx.userId(25L);
        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testRouterNumber() {

        String pattern = "  userId match ~11 => ~ip\"192.168.1.104/30\" ";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();

        ctx.userId(12L);
        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        expectInstances.add(runtimeInstance2);
        expectInstances.add(runtimeInstance3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    /**
     * 测试黑名单 -> 可以根据 掩码来
     */
    @Test
    public void testRouterIp() {
        String pattern = "  calleeIp match ip'192.168.1.101/24' => ip\"192.168.2.105/30\" ";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();


//        ctx.calleeIp("192.168.1.97   ");
        ctx.calleeIp(IPUtils.transferIp("192.168.1.101"));
        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 测试服务降级
     */
    @Test
    public void testRouterServiceDown() {
        String pattern = "  otherwise => ip\"192.168.2.105/30\" ";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();


        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 测试空字符串
     */
    @Test
    public void testRouterBlank() {
        StringBuilder builder = new StringBuilder();
        builder.append("\r\n");
        builder.append("method match 'register' => ip'192.168.10.12'" + "\r\n");
        builder.append("\r\n");
        builder.append("method match 'register' => ip'192.168.10.12'" + "\r\n");


        List<Route> routes = RoutesExecutor.parseAll(builder.toString());
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.methodName("register");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    /**
     * 测试多个换行符
     */
    @Test
    public void testRouterEOL() {

        String pattern = "otherwise => ip\"192.168.2.105/30\"\r\n";
        StringBuilder sb = new StringBuilder(32);
        sb.append(pattern);
        sb.append("\r\n");
        sb.append("\r\n");
        List<Route> routes = RoutesExecutor.parseAll(sb.toString());
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();


        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    /**
     * 测试 cookies 路由解析
     */
    @Test
    public void testCookieRouter() {
        String pattern = "cookie_storeId match 11866600  => ip\"192.168.1.101\"\n" +
                "otherwise => ~ip\"192.168.10.126\" ";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "11866600");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    /**
     * 测试路由解析 没有空格区分 ,测试 cookies
     */
    @Test
    public void testErrorRouteLexer() {
        String pattern = "cookie_storeId match 11866600 ; method match  \"updateOrderMemberId\" => ip\"192.168.1.101\"\n" +
                "otherwise => ~ip\"192.168.10.126\" ";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "11866600");
        ctx.methodName("updateOrderMemberId");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 测试 otherwise 不走两个或多个 ip
     */
    @Test
    public void testOtherWiseTwoIp() {
        //"192.168.1.101",
        String pattern = "otherwise => ~ip\"192.168.1.101\" , ~ip\"192.168.1.102\" , ~ip\"192.168.1.103\" ";


        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "11866600");
        ctx.methodName("updateOrderMemberId");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
//        expectInstances.add(runtimeInstance3);
        expectInstances.add(runtimeInstance4);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testOtherWiseIpMark() {
        //"192.168.1.101",
        String pattern = "otherwise => ~ip\"192.168.1.101/24\"    ";


        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "11866600");
        ctx.methodName("updateOrderMemberId");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = Collections.emptyList();

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testMoreThenError() {
        String pattern = "cookie_storeId match 11866600  => ip\"192.168.1.101\"   => ip\"192.168.1.102\"\n" +
                "otherwise => ~ip\"192.168.10.126\" ";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "118666200");
        ctx.methodName("updateOrderMemberId");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testMoreThenIp2(){
        String pattern = "cookie_storeId match 11888900 , 11728901 , 11735000 , 11799200 ; method match \"reverseOrderPayment\" => ip\"1.1.1.1\"\n" +
                "method match \"createOfflineOrder\"  => ip\"192.168.10.126\"\n" +
                "method match \"getServiceMetadata\" , \"createMiniOrders\" , \"createDoneOrderPayments\" , \"createCanceledOrderPayments\" => ip\"192.168.10.130\"\n" +
                "cookie_storeId match 11866600 , 11799200 , 11735000 , 11739600  =>  ip\"192.168.10.130\"=> ip\"192.168.10.130\"\n" +
                "cookie_storeId match 11888900 , 11728901 , 11735000 , 11799200 => ip\"192.168.10.126\"\n" +
                "otherwise => ~ip\"192.168.10.0/24\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "118666200");
        ctx.methodName("updateOrderMemberId");

        List<RuntimeInstance> prepare = prepare(ctx, routes);


        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());

    }


}
