import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午10:00
 */
public class TestRouterRuntimeList {


    RuntimeInstance runtimeInstance1 = new RuntimeInstance("com.maple.Uservice", "192.168.1.101", 9090, "1.0.0");
    RuntimeInstance runtimeInstance2 = new RuntimeInstance("com.maple.Uservice", "192.168.1.102", 9091, "1.0.0");
    RuntimeInstance runtimeInstance3 = new RuntimeInstance("com.maple.Uservice", "192.168.1.103", 9092, "1.0.0");
    RuntimeInstance runtimeInstance4 = new RuntimeInstance("com.maple.Uservice", "192.168.1.104", 9093, "1.0.0");


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
     */
    @Test
    public void testRouter() {
        String onePattern_oneMatcher = "method match 'getFoo' , 'setFoo' ; version match '1.0.0' => ip'192.168.1.101' , ~ip'192.168.1.103' ";
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
    public void testRouter2() {
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


    @Test
    public void testRouterMode2() {
        //TODO
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
        ctx.calleeIp("192.168.1.101   ");
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

}
