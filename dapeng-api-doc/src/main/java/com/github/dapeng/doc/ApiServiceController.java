package com.github.dapeng.doc;


import com.github.dapeng.doc.cache.ServiceCache;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.util.MetaDataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Api服务Controller
 *
 * @author craneding
 * @date 15/9/29
 */
@Controller
@RequestMapping(value = "api")
public class ApiServiceController {

    @Autowired
    private ServiceCache serviceCache;

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("tagName", "api");
    }

    @RequestMapping(value = "index", method = RequestMethod.GET)
    public String api(HttpServletRequest request) {
        Map<String, Service> services = serviceCache.getServices();

        request.setAttribute("services", services.values());

        return "api/api";
    }

    @RequestMapping(value = "service/{serviceName}/{version}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String service(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version) {
        request.setAttribute("service", serviceCache.getService(serviceName, version));
        request.setAttribute("services", serviceCache.getServices().values());
        return "api/service";
    }

    @RequestMapping(value = "method/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String method(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        Service service = serviceCache.getService(serviceName, version);

        Method seleted = MetaDataUtil.findMethod(methodName, service);
        List<Method> methods = service.getMethods();

        Collections.sort(methods, Comparator.comparing(Method::getName));

        request.setAttribute("service", service);
        request.setAttribute("methods", methods);
        request.setAttribute("method", seleted);
        return "api/method";
    }

    @RequestMapping(value = "findmethod/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @ResponseBody
    public Method findMethod(@PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        Service service = serviceCache.getService(serviceName, version);

        return MetaDataUtil.findMethod(methodName, service);
    }

    @RequestMapping(value = "struct/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String struct(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        Service service = serviceCache.getService(serviceName, version);

        request.setAttribute("struct", MetaDataUtil.findStruct(ref, service));
        request.setAttribute("service", service);
        request.setAttribute("structs", service.getStructDefinitions());
        return "api/struct";
    }

    @RequestMapping(value = "findstruct/{serviceName}/{version}/{fullStructName}", method = RequestMethod.GET)
    @ResponseBody
    public Struct findStruct(@PathVariable String serviceName, @PathVariable String version, @PathVariable String fullStructName) {
        Service service = serviceCache.getService(serviceName, version);

        return MetaDataUtil.findStruct(fullStructName, service);
    }

    @RequestMapping(value = "enum/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String anEnum(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        Service service = serviceCache.getService(serviceName, version);

        request.setAttribute("anEnum", MetaDataUtil.findEnum(ref, service));

        request.setAttribute("service", service);
        request.setAttribute("enums", service.getEnumDefinitions());
        return "api/enum";
    }

    @RequestMapping(value = "test/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @Transactional(value = "isuwang_api", rollbackFor = Exception.class)
    public String goTest(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {

        Service service = serviceCache.getService(serviceName, version);

        request.setAttribute("service", service);
        request.setAttribute("method", MetaDataUtil.findMethod(methodName, service));
        request.setAttribute("services", serviceCache.getServices().values());
        return "api/test";
    }

    @RequestMapping(value = "findService/{serviceName}/{version}", method = RequestMethod.GET)
    @ResponseBody
    public Service findService(@PathVariable String serviceName, @PathVariable String version) {
        return serviceCache.getService(serviceName, version);
    }

    @RequestMapping(value = "findServiceAfterRefresh/{serviceName}/{version}/{refresh}", method = RequestMethod.GET)
    @ResponseBody
    public Service findService(@PathVariable String serviceName, @PathVariable String version, @PathVariable boolean refresh) {
//        if (refresh) {
//            serviceCache.reloadServices();
//        }
        return serviceCache.getService(serviceName, version);
    }
}
