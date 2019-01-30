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

public enum TGlobalTransactionProcessStatus implements TEnum {

    /**
     *
     **/
    New(1),

    /**
     *
     **/
    Success(2),

    /**
     *
     **/
    Fail(3),

    /**
     *
     **/
    Unknown(4),

    /**
     *
     **/
    HasRollback(5);


    private final int value;

    private TGlobalTransactionProcessStatus(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    public static TGlobalTransactionProcessStatus findByValue(int value) {
        switch (value) {

            case 1:
                return New;

            case 2:
                return Success;

            case 3:
                return Fail;

            case 4:
                return Unknown;

            case 5:
                return HasRollback;

            default:
                return null;
        }
    }
}
      