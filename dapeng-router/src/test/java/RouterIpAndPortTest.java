import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.RuntimeInstance;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由 ip port 单元测试
 */
public class RouterIpAndPortTest {
    private RuntimeInstance runtimeInstance1_9090 = new RuntimeInstance("com.maple.Uservice", "192.168.1.101", 9090, "1.0.0");
    private RuntimeInstance runtimeInstance1_9091 = new RuntimeInstance("com.maple.Uservice", "192.168.1.101", 9091, "1.0.0");
    private RuntimeInstance runtimeInstance2_____ = new RuntimeInstance("com.maple.Uservice", "192.168.1.102", 9092, "1.0.0");
    private RuntimeInstance runtimeInstance3_9087 = new RuntimeInstance("com.maple.Uservice", "192.168.1.103", 9087, "1.0.0");
    private RuntimeInstance runtimeInstance3_9091 = new RuntimeInstance("com.maple.Uservice", "192.168.1.103", 9091, "1.0.0");

    private List<RuntimeInstance> prepare(InvocationContextImpl ctx, List<Route> routes) {
        List<RuntimeInstance> instances = new ArrayList<>();
        instances.add(runtimeInstance1_9090);
        instances.add(runtimeInstance1_9091);
        instances.add(runtimeInstance2_____);
        instances.add(runtimeInstance3_9087);
        instances.add(runtimeInstance3_9091);
        return RoutesExecutor.executeRoutes(ctx, routes, instances);
    }


    /**
     * 1.more useful
     */
    @Test
    public void testMore() {
        String pattern = "  otherwise  => ip\"192.168.1.101:9090\" , ip\"192.168.1.102\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9090);
        expectInstances.add(runtimeInstance2_____);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 1.more useful
     */
    @Test
    public void testMore2() {
        String pattern = "  otherwise  => ip\"192.168.1.101:9090\" , ip\"192.168.1.103\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9090);
        expectInstances.add(runtimeInstance3_9087);
        expectInstances.add(runtimeInstance3_9091);

        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 2.精确指定到一个节点的一个端口
     */
    @Test
    public void testIpPortOne() {
        String pattern = "  otherwise  => ip\"192.168.1.101:9090\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9090);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 1.不指定节点，根据IP来
     */
    @Test
    public void testIpNoPort() {
        String pattern = "  otherwise  => ip\"192.168.1.101\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9090);
        expectInstances.add(runtimeInstance1_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 1.指定端口，根据 not 规则
     */
    @Test
    public void testIpANdPortAndNot() {
        String pattern = "  otherwise  => ~ip\"192.168.1.101:9090\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9091);
        expectInstances.add(runtimeInstance2_____);
        expectInstances.add(runtimeInstance3_9087);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 1.指定端口，根据 not 规则
     */
    @Test
    public void testIpNoPortAndNot() {
        String pattern = "  otherwise  => ~ip\"192.168.1.101\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance2_____);
        expectInstances.add(runtimeInstance3_9087);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 1.指定 ip 端口，根据 mask 进行匹配
     */
    @Test
    public void testIpAndPortAndMask() {
        String pattern = "  otherwise  => ip\"192.168.1.101/24:9091\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9091);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 1.指定 ip 端口，根据 mask 进行匹配 \ not规则
     */
    @Test
    public void testIpAndPortAndMaskAndNot() {
        String pattern = "  otherwise  => ~ip\"192.168.1.101/24:9091\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9090);
        expectInstances.add(runtimeInstance2_____);
        expectInstances.add(runtimeInstance3_9087);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 多 ip 指定
     */
    @Test
    public void testIpAndPortMoreIp() {
        String pattern = "  otherwise  => ip\"192.168.1.101:9091\" , ip\"192.168.1.102\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9091);
        expectInstances.add(runtimeInstance2_____);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 多 ip 指定
     */
    @Test
    public void testIpAndPortMoreIpMask() {
        String pattern = "  otherwise  => ip\"192.168.1.101/24:9091\" , ip\"192.168.1.102\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9091);
        expectInstances.add(runtimeInstance2_____);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * 多 ip 指定
     */
    @Test
    public void testIpAndPortMoreIpMask2() {
        String pattern = "  otherwise  => ip\"192.168.1.101/24\" , ~ip\"192.168.1.102\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9090);
        expectInstances.add(runtimeInstance1_9091);
        expectInstances.add(runtimeInstance3_9087);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * not mode
     */
    @Test
    public void testIpAndPortNot() {
        String pattern = "  otherwise  => ~ip\"192.168.1.101\" , ~ip\"192.168.1.102\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance3_9087);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }

    /**
     * not mode
     */
    @Test
    public void testIpAndPortNotAndMore() {
        String pattern = "  otherwise  => ~ip\"192.168.1.101:9090\" , ip\"192.168.1.101/24\"";
        List<Route> routes = RoutesExecutor.parseAll(pattern);
        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<RuntimeInstance> prepare = prepare(ctx, routes);

        List<RuntimeInstance> expectInstances = new ArrayList<>();
        expectInstances.add(runtimeInstance1_9091);
        expectInstances.add(runtimeInstance2_____);
        expectInstances.add(runtimeInstance3_9087);
        expectInstances.add(runtimeInstance3_9091);
        Assert.assertArrayEquals(expectInstances.toArray(), prepare.toArray());
    }


}
