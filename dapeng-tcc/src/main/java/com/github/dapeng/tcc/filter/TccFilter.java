package com.github.dapeng.tcc.filter;

import com.github.dapeng.core.*;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.org.apache.thrift.transport.TTransport;
import com.github.dapeng.tm.TransactionManagerServiceClient;
import com.github.dapeng.tm.service.BeginGtxRequest;
import com.github.dapeng.tm.service.BeginGtxResponse;
import com.github.dapeng.tm.service.CcRequest;
import com.github.dapeng.util.TCommonTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Stack;

/**
 * Tcc Interceptor
 *
 * @author Ever
 * @date 26/1/19
 */
public class TccFilter implements Filter {
    Logger LOGGER = LoggerFactory.getLogger(TccFilter.class);
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
                Object reqArgs = ctx.getAttribute("reqArgs");
                SoaFunctionDefinition soaFunction = (SoaFunctionDefinition) ctx.getAttribute("soaFunction");
                int reqLength = (Integer) transactionContext.getAttribute("reqLength");
                BeginGtxRequest request = new BeginGtxRequest();
                request.params = Optional.of(serialReq(soaFunction, reqArgs, reqLength));
                request.setServiceName(serviceName);
                request.setVersion(versionName);
                request.setMethod(methodName);
                request.setConfirmMethod(Optional.of(tcc.confirmMethod()));
                request.setCancelMethod(Optional.of(tcc.cancelMethod()));
                request.setIsAsync(Optional.of(tcc.asynCC()));
                BeginGtxResponse beginGtxResponse = client.beginGtx(request);
                header.setTransactionId(beginGtxResponse.getGtxId());
                long stepId = beginGtxResponse.getStepId();
                if (transactionContext.getHeader().getCookie("global_stepId") != null) {
                    transactionContext.setAttribute("isGlobal", false);
                } else {
                    transactionContext.getHeader().addCookie("global_stepId", Long.toString(stepId));
                    transactionContext.setAttribute("isGlobal", true);
                }
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
                long gtxId = transactionContext.getHeader().getTransactionId().get();
                boolean isGlobal = (boolean) transactionContext.getAttribute("isGlobal");
                if (isGlobal && header.getRespCode().get().equals(SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE)) {
                    CcRequest request = new CcRequest();
                    request.setGtxId(gtxId);
                    client.confirm(request);
                }
                if (isGlobal && !header.getRespCode().get().equals(SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE)) {
                    CcRequest request = new CcRequest();
                    request.setGtxId(gtxId);
                    client.cancel(request);
                }
            }
            prev.onExit(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ByteBuffer serialReq(SoaFunctionDefinition soaFunction, Object reqArgs, int reqLength) throws SoaException {
        try {
            byte[] reqBytes = new byte[reqLength];
            TTransport transport = new TCommonTransport(reqBytes, TCommonTransport.Type.Write);
            //todo 应根据客户端带过来的协议类型来决定 参看SoaMessageBuilder
            TProtocol protocol = new TCompactProtocol(transport);
            soaFunction.reqSerializer.write(reqArgs, protocol);

            return ByteBuffer.wrap(reqBytes);
        } catch (TException e) {
            LOGGER.error("Tcc serial req failed", e);
            throw new SoaException(e);
        }
    }
}