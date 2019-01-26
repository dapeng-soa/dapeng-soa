package com.github.dapeng.tcc.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.definition.SoaFunctionDefinition;
import com.github.dapeng.core.filter.Filter;
import com.github.dapeng.core.filter.FilterChain;
import com.github.dapeng.core.filter.FilterContext;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.org.apache.thrift.transport.TTransport;
import com.github.dapeng.tm.TransactionManagerServiceClient;
import com.github.dapeng.tm.service.BeginGtxRequest;
import com.github.dapeng.tm.service.TransactionManagerService;
import com.github.dapeng.util.TCommonTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Tcc Interceptor
 *
 * @author Ever
 * @date 26/1/19
 */
public class TccFilter implements Filter {
    Logger LOGGER = LoggerFactory.getLogger(TccFilter.class);


    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
        boolean tccEnable = false;
        if (tccEnable) {
            beginGtx(ctx);
        }

        next.onEntry(ctx);
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

        prev.onExit(ctx);
    }

    private void beginGtx(FilterContext ctx) throws SoaException {
        Object reqArgs = ctx.getAttribute("reqArgs");
        SoaFunctionDefinition soaFunction = (SoaFunctionDefinition) ctx.getAttribute("soaFunction");

        TransactionContext transactionContext = (TransactionContext) ctx.getAttribute("context");
        int reqLength = (Integer)transactionContext.getAttribute("reqLength");
        TransactionManagerService tmService = new TransactionManagerServiceClient();

        BeginGtxRequest gtxRequest = new BeginGtxRequest();
        gtxRequest.params = Optional.of(serialReq(soaFunction, reqArgs, reqLength));
        // gtxRequest...
        tmService.beginGtx(gtxRequest);
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