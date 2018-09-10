package com.github.dapeng.util;

import com.github.dapeng.core.SoaBaseCodeInterface;
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
        } else if (ex instanceof SoaBaseCodeInterface) {
            soaException = new SoaException((SoaBaseCodeInterface)ex);
        } else {
            soaException = new SoaException(SoaCode.ServerUnKnown.getCode(),
                    ex.getCause() != null ? ex.getCause().toString() : (ex.getMessage() == null ? SoaCode.ServerUnKnown.getMsg() : ex.getMessage()), ex);
        }
        return soaException;
    }
}
