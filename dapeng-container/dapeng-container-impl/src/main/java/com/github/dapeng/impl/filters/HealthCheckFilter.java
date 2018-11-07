package com.github.dapeng.impl.filters;

import com.github.dapeng.api.Container;
import com.github.dapeng.api.healthcheck.DoctorFactory;
import com.github.dapeng.core.HealthCheck;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.filter.SharedChain;
import com.github.dapeng.util.DumpUtil;
import com.google.gson.Gson;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author hui
 * @date 2018/11/7 0007 9:16
 */
public class HealthCheckFilter implements Filter {
    private final Gson gson = new Gson();

    @Override
    public void onEntry(FilterContext filterContext, FilterChain next) throws SoaException {
        TransactionContext transactionContext = (TransactionContext) filterContext.getAttribute("context");
        Container container = (Container) filterContext.getAttribute("container");

        String methodName = transactionContext.getHeader().getMethodName();

        if ("echo".equals(methodName)) {

            FilterBuilder filterBuilder = new FilterBuilder();
            filterBuilder = filterBuilder.includePackage("com");//定义要扫描的包
            /**
             * 定义Reflections对象，指明"包过滤器"，以及扫描器的类型，主要把是扫描器的类型
             * 细分之后，得到对应的数据
             */
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .filterInputsBy(filterBuilder)
                    .setScanners(new MethodAnnotationsScanner().filterResultsBy(filterBuilder)).addUrls(ClasspathHelper.forPackage("com")));

            Set<Method> methods = reflections.getMethodsAnnotatedWith(HealthCheck.class);
            System.out.println("实现健康检查的method ：" + methods.size());
            System.out.println("实现健康检查的method ：" + methods);


            String echoInfo = DumpUtil.dumpThreadPool((ThreadPoolExecutor) container.getDispatcher());
            Map<String, Object> diagnoseMap = DoctorFactory.getDoctor().diagnoseReport();
            diagnoseMap.put("service", transactionContext.getHeader().getServiceName());
            diagnoseMap.put("container_info", echoInfo);
            transactionContext.setAttribute("container-threadPool-info", gson.toJson(diagnoseMap));
            transactionContext.setAttribute("dapeng_request_timestamp", System.currentTimeMillis());
            this.onExit(filterContext, getPrevChain(filterContext));
        } else {
            next.onEntry(filterContext);
        }
    }

    @Override
    public void onExit(FilterContext filterContext, FilterChain prev) throws SoaException {
        prev.onExit(filterContext);
    }

    public FilterChain getPrevChain(FilterContext filterContext) {
        SharedChain sharedChain = (SharedChain) filterContext.getAttribute("shareChain");
        return new SharedChain(sharedChain.head, sharedChain.shared, sharedChain.tail, sharedChain.size() - 4);
    }
}
