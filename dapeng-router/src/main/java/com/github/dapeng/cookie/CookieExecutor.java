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
package com.github.dapeng.cookie;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.router.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cookie注入。
 * 可通过配置一定规则，凡是符合规则的请求，都能动态的注入一个或多个cookie值。
 * 例如:
 * cookie_storeId match 100720..101000 ; method match r"list.*" => c"thread-log-level#DEBUG"
 *
 * 上述规则，当店号在[100720,101000]范围内，且请求方法名是list开头，那么就给这个请求添加一个key为thread-log-level，value为DEBUG的cookie值。
 * (上述cookie添加后，会把整个会话范围内的日志级别调整为DEBUG级别)
 *
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 5:58 PM
 */
public class CookieExecutor extends RoutesExecutor {
    private static Logger logger = LoggerFactory.getLogger(CookieExecutor.class);

    /**
     * 解析 Cookie rules cookie规则
     *
     * @param content 设置在 zk cookies 节点下的规则内容信息
     * @return
     */
    public static List<CookieRule> parseCookieRules(String content) {
        CookieParser parser = new CookieParser(new RoutesLexer(content));
        return parser.cookieRoutes();
    }

    /**
     * 执行 cookie规则 匹配，
     */
    public static void injectCookies(InvocationContextImpl ctx, List<CookieRule> rules) {
        boolean isMatched;
        for (CookieRule rule : rules) {
            try {
                isMatched = matchCondition(ctx, rule.getLeft());
                // 匹配成功，执行右边逻辑
                if (isMatched) {
                    List<CookieRight> cookieRights = rule.getCookieRightList();

                    ctx.cookies(cookieRights.stream().collect(Collectors.toMap(CookieRight::cookieKey, CookieRight::cookieValue)));

                    if (logger.isDebugEnabled()) {
                        logger.debug(CookieExecutor.class.getSimpleName() + "::cookie left " + rule.getLeft().toString() +
                                        "::cookies  size: {}, content: {}",
                                cookieRights.size(), cookieRights.toString());
                    }
                    break;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(CookieExecutor.class.getSimpleName() + "::cookie left " + rule.getLeft().toString() + ":: cookies no rule");
                    }
                }
            } catch (Throwable ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }
}
