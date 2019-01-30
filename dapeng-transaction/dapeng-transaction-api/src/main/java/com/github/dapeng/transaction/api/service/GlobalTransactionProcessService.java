
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
package com.github.dapeng.transaction.api.service;

import com.github.dapeng.core.SoaBaseCodeInterface;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.Processor;
import com.github.dapeng.core.Service;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.github.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;

import java.util.Date;

/**
 *
 **/
@Service(name="com.github.dapeng.transaction.api.service.GlobalTransactionProcessService", version = "1.0.0")
@Processor(className = "com.github.dapeng.transaction.api.GlobalTransactionProcessServiceCodec$Processor")
public interface GlobalTransactionProcessService {

    /**
     *
     **/
    TGlobalTransactionProcess create(TGlobalTransactionProcess globalTransactionProcess) throws SoaException;

    /**
     *
     **/
    void update(Integer globalTransactionProcessId, String responseJson, TGlobalTransactionProcessStatus status) throws SoaException;

    void updateExpectedStatus(Integer processId, TGlobalTransactionProcessExpectedStatus status) throws SoaException;

    void updateRedoTimes(Integer globalTransactionProcessId) throws SoaException;
}
        