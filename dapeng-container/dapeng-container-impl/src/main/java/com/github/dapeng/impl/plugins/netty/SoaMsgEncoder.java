/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.api.Container;
import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.*;
import com.github.dapeng.core.helper.DapengUtil;
import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.ExceptionUtil;
import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;

import static com.github.dapeng.api.Container.STATUS_RUNNING;
import static com.github.dapeng.api.Container.STATUS_SHUTTING;
import static com.github.dapeng.core.helper.SoaSystemEnvProperties.SOA_NORMAL_RESP_CODE;

/**
 * 响应消息编码器
 *
 * @author Ever
 */
@ChannelHandler.Sharable
public class SoaMsgEncoder extends MessageToByteEncoder<SoaResponseWrapper> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaMsgEncoder.class);

    private final Container container;

    SoaMsgEncoder(Container container) {
        this.container = container;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          SoaResponseWrapper wrapper,
                          ByteBuf out) throws Exception {
        TransactionContext transactionContext = wrapper.transactionContext;
        MDC.put(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID, transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(getClass().getSimpleName() + "::encode");
        }

        SoaHeader soaHeader = transactionContext.getHeader();
        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        //容器不是运行状态或者将要关闭状态
        if (application == null) {
            LOGGER.error(getClass() + "::encode application is null, container status:" + container.status());
            writeErrorResponse(transactionContext, out);
            return;
        }

        try {
            Optional<String> respCode = soaHeader.getRespCode();

            if (respCode.isPresent() && !respCode.get().equals(SOA_NORMAL_RESP_CODE)) {
                writeErrorResponse(transactionContext, application, out);
            } else {
                try {
                    //fix java.util.NoSuchElementException: No value present
                    Optional<BeanSerializer> serializer = wrapper.serializer;
                    Optional<Object> result = wrapper.result;

                    TSoaTransport transport = new TSoaTransport(out);
                    SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

                    updateSoaHeader(soaHeader, transactionContext);

                    messageProcessor.writeHeader(transactionContext);

                    if (serializer.isPresent() && result.isPresent()) {
                        try {
                            messageProcessor.writeBody(serializer.get(), result.get());
                        } catch (SoaException e) {
                            if (e.getCode().equals(SoaCode.StructFieldNull.getCode())) {
                                e.setCode(SoaCode.ServerRespFieldNull.getCode());
                                e.setMsg(SoaCode.ServerRespFieldNull.getMsg());
                            }
                            throw e;
                        }
                    }
                    messageProcessor.writeMessageEnd();
                    transport.flush();
                    //请求返回，容器请求数 -1
                    container.requestCounter().decrementAndGet();
                    if (LOGGER.isDebugEnabled()) {
                        String debugLog = "response[seqId:" + transactionContext.seqId() + ", respCode:" + respCode.get() + "]:"
                                + "service[" + soaHeader.getServiceName()
                                + "]:version[" + soaHeader.getVersionName()
                                + "]:method[" + soaHeader.getMethodName() + "]"
                                + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : ",")
                                + (soaHeader.getCustomerId().isPresent() ? " customerId:" + soaHeader.getCustomerId().get() : ",")
                                + " calleeTime1:" + soaHeader.getCalleeTime1().orElse(-1) + ","
                                + " calleeTime2:" + soaHeader.getCalleeTime2().orElse(-1);
                        LOGGER.debug(getClass().getSimpleName() + "::encode:" + debugLog + ", payload[seqId:" + transactionContext.seqId() + "]:\n" + result);
                        LOGGER.debug(getClass().getSimpleName() + "::encode, payloadAsByteBuf:\n" + DumpUtil.dumpToStr(out));
                    }
                } catch (Throwable e) {
                    SoaException soaException = ExceptionUtil.convertToSoaException(e);

                    soaHeader.setRespCode(soaException.getCode());
                    soaHeader.setRespMessage(soaException.getMessage());

                    transactionContext.soaException(soaException);
                    writeErrorResponse(transactionContext, application, out);
                }
            }
        } finally {
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        }
    }

    private void updateSoaHeader(SoaHeader soaHeader, TransactionContext transactionContext) {
        Long requestTimestamp = (Long) transactionContext.getAttribute("dapeng_request_timestamp");

        Long cost = System.currentTimeMillis() - requestTimestamp;
        soaHeader.setCalleeTime2(cost.intValue());
        soaHeader.setCalleeIp(Optional.of(IPUtils.transferIp(SoaSystemEnvProperties.HOST_IP)));
        soaHeader.setCalleePort(Optional.of(SoaSystemEnvProperties.SOA_CONTAINER_PORT));
        Joiner joiner = Joiner.on(":");
        soaHeader.setCalleeMid(joiner.join(soaHeader.getServiceName(), soaHeader.getMethodName(), soaHeader.getVersionName()));
        soaHeader.setCalleeTid(transactionContext.calleeTid());
    }

    /**
     * override the initialCapacity to 1024
     *
     * @param ctx
     * @param msg
     * @param preferDirect
     * @return
     * @throws Exception
     */
    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, @SuppressWarnings("unused") SoaResponseWrapper msg,
                                     boolean preferDirect) throws Exception {
        if (preferDirect) {
            return ctx.alloc().ioBuffer(5120);
        } else {
            return ctx.alloc().heapBuffer(5120);
        }
    }

    private void writeErrorResponse(TransactionContext transactionContext,
                                    Application application,
                                    ByteBuf out) {
        SoaHeader soaHeader = transactionContext.getHeader();
        SoaException soaException = transactionContext.soaException();
        if (soaException == null) {
            soaException = new SoaException(soaHeader.getRespCode().get(),
                    soaHeader.getRespMessage().orElse(SoaCode.ServerUnKnown.getMsg()));
            transactionContext.soaException(soaException);
        }

        //Reuse the byteBuf
        if (out.readableBytes() > 0) {
            out.clear();
        }

        TSoaTransport transport = new TSoaTransport(out);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        try {
            messageProcessor.writeHeader(transactionContext);
            messageProcessor.writeMessageEnd();

            transport.flush();
            MdcCtxInfoUtil.putMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID,
                    transactionContext.sessionTid().map(DapengUtil::longToHexStr).orElse("0"));
            String infoLog = "response[seqId:" + transactionContext.seqId() + ", respCode:" + soaHeader.getRespCode().get() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                    + (soaHeader.getCustomerId().isPresent() ? " customerId:" + soaHeader.getCustomerId().get() : "");
            // 根据respCode判断是否是业务异常还是运行时异常
            if (DapengUtil.isDapengCoreException(soaException)) {
                application.error(this.getClass(), infoLog, soaException);
            } else {
                application.info(this.getClass(), infoLog);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(getClass() + " " + infoLog + ", payload:\n" + soaException.getMessage());
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            //请求返回，容器请求数 -1
            container.requestCounter().decrementAndGet();
            MdcCtxInfoUtil.removeMdcToAppClassLoader(application.getAppClasssLoader(), SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        }
    }

    /**
     * application 为空。 容器不在运行状态下时，writeErrorResponse
     *
     * @param transactionContext 服务上下文信息
     * @param out                {@link ByteBuf}
     */
    private void writeErrorResponse(TransactionContext transactionContext,
                                    ByteBuf out) {
        SoaHeader soaHeader = transactionContext.getHeader();
        // make sure responseCode of error responses do not equal to SOA_NORMAL_RESP_CODE
        if (soaHeader.getRespCode().isPresent() && soaHeader.getRespCode().get().equals(SOA_NORMAL_RESP_CODE)) {
            soaHeader.setRespCode(SoaCode.ContainerStatusError.getCode());
            soaHeader.setRespMessage(SoaCode.ContainerStatusError.getMsg());
        }
        SoaException soaException = transactionContext.soaException();
        if (soaException == null) {
            soaException = new SoaException(soaHeader.getRespCode().orElse(SoaCode.ContainerStatusError.getCode()),
                    soaHeader.getRespMessage().orElse(SoaCode.ContainerStatusError.getMsg()));
            transactionContext.soaException(soaException);
        }
        //重复利用ByteBuf
        if (out.readableBytes() > 0) {
            out.clear();
        }
        TSoaTransport transport = new TSoaTransport(out);
        SoaMessageProcessor messageProcessor = new SoaMessageProcessor(transport);

        try {
            messageProcessor.writeHeader(transactionContext);
            messageProcessor.writeMessageEnd();

            transport.flush();
            String infoLog = "response[seqId:" + transactionContext.seqId() + ", respCode:" + soaHeader.getRespCode().get() + "]:"
                    + "service[" + soaHeader.getServiceName()
                    + "]:version[" + soaHeader.getVersionName()
                    + "]:method[" + soaHeader.getMethodName() + "]"
                    + (soaHeader.getOperatorId().isPresent() ? " operatorId:" + soaHeader.getOperatorId().get() : "")
                    + (soaHeader.getCustomerId().isPresent() ? " customerId:" + soaHeader.getCustomerId().get() : "");

            LOGGER.info(getClass() + " " + infoLog + ", payload:\n" + soaException.getMessage());
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            //请求返回，容器请求数 -1
            container.requestCounter().decrementAndGet();
            MDC.remove(SoaSystemEnvProperties.KEY_LOGGER_SESSION_TID);
        }
    }
}
