import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.router.Route;
import com.github.dapeng.router.RoutesExecutor;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午10:00
 */
public class TestRouter {

    @Test
    public void trstRouter() {
        String onePattern_oneMatcher = "method match \"getFoo\" , \"setFoo\" ; version match \"1.0.0\" => ip\"192.168.1.101/23\" , ~ip\"192.168.1.103/24\" ";
        List<Route> routes = RoutesExecutor.parseAll(onePattern_oneMatcher);

        InvocationContextImpl ctx = (InvocationContextImpl) InvocationContextImpl.Factory.currentInstance();
        List<String> server = new ArrayList<>();
        server.add("192.168.1.101");
        server.add("192.168.1.102");
        server.add("192.168.1.103");
        server.add("192.168.1.104");

        ctx.serviceName("getFoo");
        ctx.versionName("1.0.0");
        Set<InetAddress> execute = RoutesExecutor.execute(ctx, routes, server);
        System.out.println(execute);

    }
}
