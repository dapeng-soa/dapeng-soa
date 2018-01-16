package com.github.dapeng.core;

import com.github.dapeng.core.enums.CodecProtocol;

/**
 * 服务端上下文
 *
 * @author craneding
 * @date 15/9/24
 */
public class TransactionContext {

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private SoaHeader header;

    private Integer seqid;

    private boolean isSoaGlobalTransactional;

    private Integer currentTransactionSequence = 0;

    private Integer currentTransactionId = 0;

    public void setCodecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public CodecProtocol getCodecProtocol() {
        return codecProtocol;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public void setHeader(SoaHeader header) {
        this.header = header;
    }

    public Integer getSeqid() {
        return seqid;
    }

    public void setSeqid(Integer seqid) {
        this.seqid = seqid;
    }

    public boolean isSoaGlobalTransactional() {
        return isSoaGlobalTransactional;
    }

    public void setSoaGlobalTransactional(boolean soaGlobalTransactional) {
        isSoaGlobalTransactional = soaGlobalTransactional;
    }

    public Integer getCurrentTransactionSequence() {
        return currentTransactionSequence;
    }

    public void setCurrentTransactionSequence(Integer currentTransactionSequence) {
        this.currentTransactionSequence = currentTransactionSequence;
    }

    public Integer getCurrentTransactionId() {
        return currentTransactionId;
    }

    public void setCurrentTransactionId(Integer currentTransactionId) {
        this.currentTransactionId = currentTransactionId;
    }

    public static class Factory {
        private static ThreadLocal<TransactionContext> threadLocal = new ThreadLocal<>();

        /**
         * 确保在业务线程入口设置context
         * @return
         */
        public static TransactionContext createNewInstance() {
            assert(threadLocal.get() == null);

            TransactionContext context = new TransactionContext();
            threadLocal.set(context);
            return context;
        }

        public static TransactionContext setCurrentInstance(TransactionContext context) {
            threadLocal.set(context);

            return context;
        }

        public static TransactionContext getCurrentInstance() { //TODO remove SoaException
            TransactionContext context = threadLocal.get();

            if (context == null) {
                context = createNewInstance();

                threadLocal.set(context);
            }

            return context;
        }

        /**
         * 确保在业务线程出口清除context
         */
        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }

    /**
     * call by client checking whether thread is in container
     *
     * @return
     */
    public static boolean hasCurrentInstance() {
        if (Factory.threadLocal.get() == null)
            return false;
        else
            return true;
    }
}
