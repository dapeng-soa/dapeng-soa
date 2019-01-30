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
