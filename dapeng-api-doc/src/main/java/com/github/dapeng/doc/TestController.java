package com.github.dapeng.doc;

import com.github.dapeng.doc.cache.ServiceCache;
import com.github.dapeng.client.json.JsonPost;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.metadata.Service;
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
import java.util.Optional;

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

        InvocationContext invocationContext = InvocationContextImpl.Factory.getCurrentInstance();
        invocationContext.setServiceName(serviceName);
        invocationContext.setVersionName(versionName);
        invocationContext.setMethodName(methodName);
        invocationContext.setCallerFrom(Optional.of("TestController"));
        try {
            return jsonPost.callServiceMethod(invocationContext, jsonParameter, service);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
