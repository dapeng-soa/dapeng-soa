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

import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用MDC辅助类
 *
 * @author: zhup
 * @date: 2018/9/7 10:11
 */

public class MdcCtxInfoUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdcCtxInfoUtil.class);
    private static final Map<ClassLoader, MdcCtxInfo> mdcCtxInfoCache = new ConcurrentHashMap<>(16);

    static class MdcCtxInfo {
        final ClassLoader appClassLoader;
        final Class<?> mdcClass;
        final Method put;
        final Method remove;

        MdcCtxInfo(ClassLoader cl, Class<?> mdcClass, Method put, Method remove) {
            this.appClassLoader = cl;
            this.mdcClass = mdcClass;
            this.put = put;
            this.remove = remove;
        }
    }

    /**
     * 容器使用 appClassloader load MDC 并 根据 key {@code mdcKey } put 值到 MDC 中
     * {@link SoaSystemEnvProperties#KEY_LOGGER_SESSION_TID},{@link SoaSystemEnvProperties#THREAD_LEVEL_KEY}
     *
     * @param appClassLoader app 应用 classloader
     * @param mdcKey      mdc key
     * @param mdcValue    mdc value
     */
    public static void putMdcToAppClassLoader(ClassLoader appClassLoader, String mdcKey, String mdcValue) {
        switchMdcToAppClassLoader("put", appClassLoader, mdcKey, mdcValue);
    }

    /**
     * 容器使用 appClassloader load MDC 并 根据 key {@code mdcKey } 从 MDC 中 remove 值
     * {@link SoaSystemEnvProperties#KEY_LOGGER_SESSION_TID},{@link SoaSystemEnvProperties#THREAD_LEVEL_KEY}
     *
     * @param appClassLoader app 应用 classloader
     * @param mdcKey      mdc key
     */
    public static void removeMdcToAppClassLoader(ClassLoader appClassLoader, String mdcKey) {
        switchMdcToAppClassLoader("remove", appClassLoader, mdcKey, null);
    }


    private static void switchMdcToAppClassLoader(String methodName, ClassLoader appClassLoader, String mdcKey, String mdcValue) {
        try {
            MdcCtxInfo mdcCtxInfo = mdcCtxInfoCache.get(appClassLoader);
            if (mdcCtxInfo == null) {
                synchronized (appClassLoader) {
                    mdcCtxInfo = mdcCtxInfoCache.get(appClassLoader);
                    if (mdcCtxInfo == null) {
                        Class<?> mdcClass = appClassLoader.loadClass(MDC.class.getName());

                        mdcCtxInfo = new MdcCtxInfo(appClassLoader,
                                mdcClass,
                                mdcClass.getMethod("put", String.class, String.class),
                                mdcClass.getMethod("remove", String.class));
                        mdcCtxInfoCache.put(appClassLoader, mdcCtxInfo);
                    }
                }
            }
            if (methodName.equals("put")) {
                mdcCtxInfo.put.invoke(mdcCtxInfo.mdcClass, mdcKey, mdcValue);
            } else {
                mdcCtxInfo.remove.invoke(mdcCtxInfo.mdcClass, mdcKey);
            }
        } catch (ClassNotFoundException | NoSuchMethodException
                | IllegalAccessException |
                InvocationTargetException e) {
            LOGGER.error(appClassLoader.getClass().getSimpleName() + "::switchMdcToAppClassLoader," + e.getMessage(), e);
        }
    }


}
