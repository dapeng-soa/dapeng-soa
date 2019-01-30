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
package com.github.dapeng.router.token;

import com.github.dapeng.core.helper.IPUtils;

/**
 * 描述: Ip 词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:08
 */
public class IpToken extends SimpleToken {
    /**
     * remote server ip,Representative form by int
     */
    public final int ip;
    /**
     * remote server port default 0
     */
    public final int port;
    /**
     * remote ip mask
     */
    public final int mask;

    public static final int DEFAULT_MASK = 32;
    public static final int DEFAULT_PORT = 0;


    public IpToken(int ip, int port, int mask) {
        super(IP);
        this.ip = ip;
        this.port = port;
        this.mask = mask;
    }


    @Override
    public String toString() {
        return "IpToken[type:" + type + ", ip:" + IPUtils.transferIp(ip) + "/" + mask + ", port:" + port + " ]";
    }
}
