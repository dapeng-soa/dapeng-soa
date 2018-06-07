package com.github.dapeng.util;

import com.github.dapeng.core.SoaCode;
import com.github.dapeng.core.SoaException;

/**
 * @author Ever
 */
public class ExceptionUtil {
    public static SoaException convertToSoaException(Throwable ex) {
        SoaException soaException;
        if (ex instanceof SoaException) {
            soaException = (SoaException) ex;
        } else {
            soaException = new SoaException(SoaCode.UnKnown.getCode(),
                    ex.getCause() != null ? ex.getCause().toString() : (ex.getMessage() == null ? SoaCode.UnKnown.getMsg() : ex.getMessage()), ex);
        }
        return soaException;
    }
}
