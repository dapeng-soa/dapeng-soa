package com.github.dapeng.doc;

import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.doc.cache.ServiceCache;
import com.github.dapeng.json.JsonPost;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 测试Controller
 *
 * @author tangliu
 * @date 15/10/8
 */
@Controller
@RequestMapping("test")
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("tagName", "test");
    }

    @Autowired
    private ServiceCache serviceCache;

    private JsonPost jsonPost = new JsonPost(SoaSystemEnvProperties.SOA_CONTAINER_IP, SoaSystemEnvProperties.SOA_CONTAINER_PORT, true);

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public String test(HttpServletRequest req) {

        String jsonParameter = req.getParameter("parameter");
        String serviceName = req.getParameter("serviceName");
        String versionName = req.getParameter("version");
        String methodName = req.getParameter("methodName");

        Service service = serviceCache.getService(serviceName, versionName);

        InvocationContext invocationCtx = InvocationContextImpl.Factory.createNewInstance();
        invocationCtx.setServiceName(serviceName);
        invocationCtx.setVersionName(versionName);
        invocationCtx.setMethodName(methodName);
        invocationCtx.setCallerFrom(Optional.of("JsonCaller"));

        fillInvocationCtx(invocationCtx, req);

        try {
            return jsonPost.callServiceMethod(invocationCtx, jsonParameter, service);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            InvocationContextImpl.Factory.removeCurrentInstance();
        }

        return null;
    }

    private void fillInvocationCtx(InvocationContext invocationCtx, HttpServletRequest req) {
        Set<String> parameters = req.getParameterMap().keySet();
        if (parameters.contains("calleeIp")) {
            invocationCtx.setCalleeIp(Optional.of(req.getParameter("calleeIp")));
        }

        if (parameters.contains("calleePort")) {
            invocationCtx.setCalleePort(Optional.of(Integer.valueOf(req.getParameter("calleePort"))));
        }

        if (parameters.contains("callerIp")) {
            invocationCtx.setCallerIp(Optional.of(req.getParameter("callerIp")));
        }

        if (parameters.contains("callerFrom")) {
            invocationCtx.setCallerFrom(Optional.of(req.getParameter("callerFrom")));
        }

        if (parameters.contains("customerName")) {
            invocationCtx.setCustomerName(Optional.of(req.getParameter("customerName")));
        }

        if (parameters.contains("customerId")) {
            invocationCtx.setCustomerId(Optional.of(Integer.valueOf(req.getParameter("customerId"))));
        }

        if (parameters.contains("operatorId")) {
            invocationCtx.setOperatorId(Optional.of(Integer.valueOf(req.getParameter("operatorId"))));
        }
    }
}
