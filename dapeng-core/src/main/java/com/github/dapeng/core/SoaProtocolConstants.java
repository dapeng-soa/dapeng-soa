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
package com.github.dapeng.core;

/**
 * Dapeng soa protocol constants
 * length(4) stx(1) version(1) protocol(1) seqid(4) header(...) body(...) etx(1)
 * @author ever
 * @date 2018/03/31
 */
public class SoaProtocolConstants {
    /**
     * Frame begin flag
     */
    public static final byte STX = 0x02;
    /**
     * Frame end flag
     */
    public static final byte ETX = 0x03;
    /**
     * Soa version
     */
    public static final byte VERSION = 1;
}
