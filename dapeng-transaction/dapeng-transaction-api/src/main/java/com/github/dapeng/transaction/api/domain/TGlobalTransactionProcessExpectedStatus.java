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
package com.github.dapeng.transaction.api.domain;

import com.github.dapeng.org.apache.thrift.TEnum;

public enum TGlobalTransactionProcessExpectedStatus implements TEnum {

    /**
     *
     **/
    Success(1),

    /**
     *
     **/
    HasRollback(2);


    private final int value;

    private TGlobalTransactionProcessExpectedStatus(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    public static TGlobalTransactionProcessExpectedStatus findByValue(int value) {
        switch (value) {

            case 1:
                return Success;

            case 2:
                return HasRollback;

            default:
                return null;
        }
    }
}
      