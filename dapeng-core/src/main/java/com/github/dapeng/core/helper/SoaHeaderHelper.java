package com.github.dapeng.core.helper;

import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.TransactionContext;

import java.util.Optional;

/**
 * Created by tangliu on 2016/5/16.
 */
public class SoaHeaderHelper {

    /**
     * 服务端获取soaHeader
     *
     * @param setDefalutIfEmpty 是否需要设置默认的customer和operator，如果header中没有值
     * @return
     */
    public static SoaHeader getSoaHeader(boolean setDefalutIfEmpty) {
        TransactionContext context = TransactionContext.Factory.getCurrentInstance();

        if (context.getHeader() == null) {
            SoaHeader header = new SoaHeader();
            context.setHeader(header);
        }

        if (setDefalutIfEmpty) {
            resetSoaHeader(context.getHeader());
        }

        return context.getHeader();
    }

    /**
     * 设置默认的customer和operator
     *
     * @param header
     */
    public static void resetSoaHeader(SoaHeader header) {

        if (!header.getOperatorId().isPresent()) {
            header.setOperatorId(Optional.of(0));
            header.setOperatorName(Optional.of("0"));
        }

        if (!header.getCustomerId().isPresent()) {
            header.setCustomerId(Optional.of(0));
            header.setCustomerName(Optional.of("0"));
        }
    }

}
