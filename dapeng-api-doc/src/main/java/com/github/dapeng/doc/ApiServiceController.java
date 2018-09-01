package com.github.dapeng.doc;


import com.github.dapeng.core.metadata.TEnum;
import com.github.dapeng.doc.cache.ServiceCache;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.doc.dto.EventDto;
import com.github.dapeng.doc.dto.EventVo;
import com.github.dapeng.doc.util.ServiceAnnotationsUtil;
import com.github.dapeng.doc.util.ServiceJsonUtil;
import com.github.dapeng.json.OptimizedMetadata;
import com.github.dapeng.util.MetaDataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

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
        Map<String, OptimizedMetadata.OptimizedService> services = serviceCache.getServices();
        Map<String, Collection<Service>> groupedServices = ServiceAnnotationsUtil.groupingServices(services.values());
        request.setAttribute("services", services.values().stream().map(OptimizedMetadata.OptimizedService::getService).collect(Collectors.toList()));
        request.setAttribute("groupedServices", groupedServices);
        return "api/api";
    }

    @RequestMapping(value = "service/{serviceName}/{version}", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    public String service(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);
        List<EventVo> events = ServiceAnnotationsUtil.findEvents(optimizedService.getService());
        request.setAttribute("events", events);
        request.setAttribute("service", ServiceAnnotationsUtil.excludeMethods(optimizedService.getService()));
        request.setAttribute("services", serviceCache.getServices().values().stream().map(OptimizedMetadata.OptimizedService::getService).collect(Collectors.toList()));
        return "api/service";
    }

    @RequestMapping(value = "method/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    public String method(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        Method seleted = optimizedService.getMethodMap().get(methodName);
        List<Method> methods = optimizedService.getService().getMethods();

        Collections.sort(methods, Comparator.comparing(Method::getName));
        List<EventDto> events = ServiceAnnotationsUtil.findEventsByMethod(seleted);

        request.setAttribute("events", events);
        request.setAttribute("service", optimizedService.getService());
        request.setAttribute("methods", methods);
        request.setAttribute("method", seleted);
        return "api/method";
    }

    @RequestMapping(value = "findmethod/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @ResponseBody
    public Method findMethod(@PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        return optimizedService.getMethodMap().get(methodName);
    }

    @RequestMapping(value = "struct/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    public String struct(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        request.setAttribute("struct", optimizedService.getOptimizedStructs().get(ref).getStruct());
        request.setAttribute("service", optimizedService.getService());
        request.setAttribute("structs", optimizedService.getService().getStructDefinitions());
        return "api/struct";
    }

    @RequestMapping(value = "findstruct/{serviceName}/{version}/{fullStructName}", method = RequestMethod.GET)
    @ResponseBody
    public Struct findStruct(@PathVariable String serviceName, @PathVariable String version, @PathVariable String fullStructName) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        return optimizedService.getOptimizedStructs().get(fullStructName).getStruct();
    }

    @RequestMapping(value = "enum/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    public String anEnum(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        request.setAttribute("anEnum", optimizedService.getEnumMap().get(ref));

        request.setAttribute("service", optimizedService.getService());
        request.setAttribute("enums", optimizedService.getService().getEnumDefinitions());
        return "api/enum";
    }

    @RequestMapping(value = "findEnum/{serviceName}/{version}/{ref}", method = RequestMethod.GET)
    @ResponseBody
    public TEnum findEnum(@PathVariable String serviceName, @PathVariable String version, @PathVariable String ref) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        return optimizedService.getEnumMap().get(ref);
    }

    @RequestMapping(value = "test/{serviceName}/{version}/{methodName}", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    public String goTest(HttpServletRequest request, @PathVariable String serviceName, @PathVariable String version, @PathVariable String methodName) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);

        request.setAttribute("service", optimizedService.getService());
        request.setAttribute("method", optimizedService.getMethodMap().get(methodName));
        request.setAttribute("services", serviceCache.getServices().values().stream().map(x->x.getService()).collect(Collectors.toList()));
        return "api/test";
    }

    @RequestMapping(value = "findService/{serviceName}/{version}", method = RequestMethod.GET)
    @ResponseBody
    public Service findService(@PathVariable String serviceName, @PathVariable String version) {
        return serviceCache.getService(serviceName, version).getService();
    }

    @RequestMapping(value = "findServiceAfterRefresh/{serviceName}/{version}/{refresh}", method = RequestMethod.GET)
    @ResponseBody
    public Service findService(@PathVariable String serviceName, @PathVariable String version, @PathVariable boolean refresh) {
//        if (refresh) {
//            serviceCache.reloadServices();
//        }
        return serviceCache.getService(serviceName, version).getService();
    }

    /**
     * 解析枚举信息，转为Json格式
     *
     * @param serviceName 服务名
     * @param version     版本号
     * @return Json 字符串
     * @author maple.lei
     */
    @RequestMapping(value = "enum/{serviceName}/{version}/jsonEnum", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public Object getEnumJson(@PathVariable String serviceName, @PathVariable String version) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);
        Map<String, Object> stringObjectMap = ServiceJsonUtil.executeJson(optimizedService.getService());
        return stringObjectMap.get(ServiceJsonUtil.JSONOBJ);

    }

    @RequestMapping(value = "enum/{serviceName}/{version}/jsonEnumString", method = RequestMethod.GET)
    @Transactional(rollbackFor = Exception.class)
    @ResponseBody
    public String getEnumJsonString(@PathVariable String serviceName, @PathVariable String version) {
        OptimizedMetadata.OptimizedService optimizedService = serviceCache.getService(serviceName, version);
        Map<String, Object> stringObjectMap = ServiceJsonUtil.executeJson(optimizedService.getService());

        return (String) stringObjectMap.get(ServiceJsonUtil.JSONSTR);
    }
}
