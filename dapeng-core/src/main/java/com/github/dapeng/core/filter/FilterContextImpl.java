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
package com.github.dapeng.core.filter;


import java.util.HashMap;
import java.util.Map;

/**
 * @author lihuimin
 * @date 2017/12/11
 */
public class FilterContextImpl implements FilterContext {

    private Map<Filter, Map<String, Object>> attachmentsWithFilter = new HashMap<>();

    private Map<String, Object> attachments = new HashMap<>();

    @Override
    public void setAttach(Filter filter, String key, Object value) {
        Map<String, Object> attches = attachmentsWithFilter.get(filter);
        if (attches == null) {
            attches = new HashMap<>();
            attachmentsWithFilter.put(filter, attches);
        }
        attches.put(key, value);
    }

    @Override
    public Object getAttach(Filter filter, String key) {
        Map<String, Object> attaches = attachmentsWithFilter.get(filter);
        if (attaches != null)
            return attaches.get(key);
        else return null;
    }

    @Override
    public void setAttribute(String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<>();
        }
        attachments.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "[attachmentsWithFilter:[");
        for (Filter filter: attachmentsWithFilter.keySet()) {
            sb.append(filter.toString() + ":[" + map2str(attachmentsWithFilter.get(filter)) + "],");
        }
        sb.append("],attachments[").append(map2str(attachments)).append("]]");
        return  sb.toString();
    }

    private String map2str(Map<String, Object> map) {
        StringBuilder buffer = new StringBuilder();
        for (String key : map.keySet()) {
            buffer.append(key + ":" + map.get(key) + ",");
        }

        return buffer.toString();
    }

}
