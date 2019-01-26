package com.github.dapeng.impl.filters;

import com.github.dapeng.core.*;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.tm.TransactionManagerServiceClient;
import com.github.dapeng.tm.service.BeginGtxRequest;
import com.github.dapeng.tm.service.BeginGtxResponse;
import com.github.dapeng.tm.service.cancelRequest;
import com.github.dapeng.tm.service.confirmRequest;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.Stack;

/**
 * @Author: zhup
 * @Date: 2019/1/17 2:23 PM
 */


public class TCCFilter implements Filter {


    private TransactionManagerServiceClient client = new TransactionManagerServiceClient();

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        try {
            TransactionContext transactionContext = (TransactionContext) ctx.getAttribute("context");
            Application application = (Application) ctx.getAttribute("application");
            SoaHeader header = transactionContext.getHeader();
            String methodName = header.getMethodName();
            String versionName = header.getVersionName();
            String serviceName = header.getServiceName();
            Optional<ServiceInfo> serviceInfo = application.getServiceInfo(serviceName, versionName);

            //TODO 处理optional.empty的情况
            TCC tcc = serviceInfo.get().tccMap.get(methodName);
            if (tcc != null) {
                ByteBuf req_param = (ByteBuf) transactionContext.getAttribute("REQ_PARAM");
                BeginGtxRequest request = new BeginGtxRequest();
                request.setServiceName(serviceName);
                request.setVersion(versionName);
                request.setMethod(methodName);
                request.setConfirmMethod(Optional.of(tcc.confirmMethod()));
                request.setCancelMethod(Optional.of(tcc.cancelMethod()));
                request.setParams(Optional.of(req_param.nioBuffer()));
                BeginGtxResponse beginGtxResponse = client.beginGtx(request);
                String gtxId = String.valueOf(beginGtxResponse.getGtxId());
                long stepId = beginGtxResponse.getStepId();
                header.addCookie("gtxId", gtxId);
                //构建子事务序列栈
                Stack stack = null;
                if (transactionContext.getAttribute("stack") != null) {
                    stack = (Stack) transactionContext.getAttribute("stack");
                } else {
                    stack = new Stack();
                }
                stack.push(stepId);
            }
            next.onEntry(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
        try {
            TransactionContext transactionContext = (TransactionContext) ctx.getAttribute("context");
            Application application = (Application) ctx.getAttribute("application");
            SoaHeader header = transactionContext.getHeader();
            String methodName = header.getMethodName();
            String versionName = header.getVersionName();
            String serviceName = header.getServiceName();
            Optional<ServiceInfo> serviceInfo = application.getServiceInfo(serviceName, versionName);
            TCC tcc = serviceInfo.get().tccMap.get(methodName);
            if (tcc != null) {
                long gtxId = Long.parseLong(transactionContext.getAttribute("gtxId").toString());
                Stack stack = (Stack) transactionContext.getAttribute("stack");
                stack.pop();
                if (stack.empty() && header.getRespCode().equals(SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE)) {
                    confirmRequest request = new confirmRequest();
                    request.setGtxId(gtxId);
                    client.confirm(request);
                } else {
                    cancelRequest request = new cancelRequest();
                    request.setGtxId(gtxId);
                    client.cancel(request);
                }
            }
            prev.onExit(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
