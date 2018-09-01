package com.github.dapeng.doc;

import com.github.dapeng.client.netty.JsonPost;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.doc.cache.ServiceCache;
import com.github.dapeng.json.OptimizedMetadata;
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

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public String test(HttpServletRequest req) {

        String jsonParameter = req.getParameter("parameter");
        String serviceName = req.getParameter("serviceName");
        String versionName = req.getParameter("version");
        String methodName = req.getParameter("methodName");

        OptimizedMetadata.OptimizedService service = serviceCache.getService(serviceName, versionName);

        InvocationContext invocationCtx = InvocationContextImpl.Factory.createNewInstance();
        invocationCtx.sessionTid(DapengUtil.generateTid());
        fillInvocationCtx(invocationCtx, req);

        JsonPost jsonPost = new JsonPost(serviceName, versionName, methodName, true);

        if (null == jsonParameter || "".equals(jsonParameter.trim())) {
            jsonParameter = "{}";
        }

        try {
            return jsonPost.callServiceMethod(jsonParameter, service);
        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");

        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            return String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
        } finally {
            InvocationContextImpl.Factory.removeCurrentInstance();
        }
    }

    private void fillInvocationCtx(InvocationContext invocationCtx, HttpServletRequest req) {
        invocationCtx.callerMid(req.getRequestURI());
        invocationCtx.userIp(IPUtils.transferIp(req.getRemoteAddr()));
        Set<String> parameters = req.getParameterMap().keySet();
        if (parameters.contains("calleeIp")) {
            invocationCtx.calleeIp(IPUtils.transferIp(req.getParameter("calleeIp")));
        }

        if (parameters.contains("calleePort")) {
            invocationCtx.calleePort(Integer.valueOf(req.getParameter("calleePort")));
        }

        if (parameters.contains("operatorId")) {
            invocationCtx.operatorId(Long.valueOf(req.getParameter("operatorId")));
        }

        if (parameters.contains("userId")) {
            invocationCtx.userId(Long.valueOf(req.getParameter("userId")));
        }
    }
}