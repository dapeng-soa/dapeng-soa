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

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.TransactionContext;

import java.util.Optional;

/**
 * Response wrapper
 * @author Ever
 */
public class SoaResponseWrapper {
    /**
     * TransactionContext
     */
    final TransactionContext transactionContext;
    /**
     * response message if exists
     */
    final Optional<Object> result;
    /**
     * codec for the response
     */
    final Optional<BeanSerializer> serializer;

    public SoaResponseWrapper(TransactionContext transactionContext, Optional<Object> result, Optional<BeanSerializer> serializer) {

        this.transactionContext = transactionContext;
        this.result = result;
        this.serializer = serializer;
    }
}
