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
package com.github.dapeng.transaction.api;

import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;

/**
 * Soa Transactional Manager
 *
 * @author craneding
 * @date 16/4/11
 */
public class GlobalTransactionFactory {

    private static GlobalTransactionService globalTransactionService;

    private static GlobalTransactionProcessService globalTransactionProcessService;

    public static GlobalTransactionService getGlobalTransactionService() {
        return globalTransactionService;
    }

    public static void setGlobalTransactionService(GlobalTransactionService globalTransactionService) {
        GlobalTransactionFactory.globalTransactionService = globalTransactionService;
    }

    public static GlobalTransactionProcessService getGlobalTransactionProcessService() {
        return globalTransactionProcessService;
    }

    public static void setGlobalTransactionProcessService(GlobalTransactionProcessService globalTransactionProcessService) {
        GlobalTransactionFactory.globalTransactionProcessService = globalTransactionProcessService;
    }

}
