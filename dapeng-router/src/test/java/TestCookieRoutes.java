import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述: test cookie_ 形式的路由
 *
 * @author hz.lei
 * @date 2018年04月13日 下午10:00
 */
public class TestCookieRoutes {

    private RuntimeInstance runtimeInst1 = new RuntimeInstance("com.maple.HelloService", "192.168.1.101", 9090, "1.0.0");
    private RuntimeInstance runtimeInst2 = new RuntimeInstance("com.maple.HelloService", "192.168.1.102", 9091, "1.0.0");
    private RuntimeInstance runtimeInst3 = new RuntimeInstance("com.maple.HelloService", "192.168.1.103", 9092, "1.0.0");
    private RuntimeInstance runtimeInst4 = new RuntimeInstance("com.maple.HelloService", "192.168.1.104", 9093, "1.0.0");


    public List<RuntimeInstance> prepare(InvocationContextImpl ctx, List<Route> routes) {
        List<RuntimeInstance> instances = new ArrayList<>();
        instances.add(runtimeInst1);
        instances.add(runtimeInst2);
        instances.add(runtimeInst3);
        instances.add(runtimeInst4);
        return RoutesExecutor.executeRoutes(ctx, routes, instances);
    }

    @Test
    public void testSimpleCookieName() {
        String pattern = "cookie_name  match r'maple'  => ip\"192.168.1.102\"";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("name", "maple");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInst2);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


    @Test
    public void testSimpleCookieMultiply() {
        String pattern = "cookie_name  match r'maple' ; cookie_age match 26  => ip\"192.168.1.102\"";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("name", "maple");
        ctx.setCookie("age", "26");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInst2);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testSimpleCookieMultiply2() {
        //不支持这种写法
//        String pattern = "cookie_name match r'maple' , cookie_age match 26  => ip\"192.168.1.103\"";
        String pattern = "cookie_name match 'maple','struy' => ip\"192.168.1.103\"\n" +
                "cookie_age match 26 => ip\"192.168.1.103\" ";


        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
//        ctx.setCookie("name", "maple");
        ctx.setCookie("age", "26");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInst3);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    @Test
    public void testRegexCookieTest() {
        String pattern = "  cookie_storeId  match r'100.*'  => ip\"192.168.1.101\"";

        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        ctx.setCookie("storeId", "100201");
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInst1);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


}
