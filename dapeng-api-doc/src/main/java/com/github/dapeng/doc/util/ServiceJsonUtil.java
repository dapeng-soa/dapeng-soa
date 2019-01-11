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
package com.github.dapeng.doc.util;

import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.TEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author with struy.
 * Create by 2018/2/25 18:05
 * email :yq1724555319@gmail.com
 */

public class ServiceJsonUtil {

    public final static String JSONOBJ = "JsonObj";
    public final static String JSONSTR = "JsonStr";

    public static Map<String, Object> executeJson(Service service) {
        Map<String, Map<String, EnumItem>> enumMap = new HashMap<>();
        List<TEnum> enumDefinitions = service.getEnumDefinitions();
        Map<String, Object> returnMap = new HashMap<>(2);

        if (null != enumDefinitions && enumDefinitions.size() > 0) {
            StringBuilder json = new StringBuilder();
            json.append("{\r\n");
            for (TEnum tEnum : enumDefinitions) {
                json.append("\t'" + tEnum.getName() + "' : {\r\n");

                Map<String, EnumItem> items = new HashMap<>();
                List<TEnum.EnumItem> enumItems = tEnum.getEnumItems();

                for (TEnum.EnumItem enumItem : enumItems) {

                    EnumItem item = new EnumItem(enumItem.getLabel());
                    item.setValue(enumItem.getLabel());
                    item.setLabel(enumItem.getDoc().trim());
                    items.put(enumItem.getLabel(), item);

                    json.append("\t\t'" + enumItem.getLabel() + "' : {\r\n");
                    json.append("\t\t\t'value' : ").append("'" + enumItem.getLabel() + "',\r\n");
                    json.append("\t\t\t'label' : ").append("'" + enumItem.getDoc().trim() + "'");
                    json.append("\r\n\t\t}");
                    json.append(",\r\n");
                }
                json.deleteCharAt(json.lastIndexOf(","));
                json.append("\t}");
                json.append(",\r\n");
                enumMap.put(tEnum.getName(), items);
            }
            json.deleteCharAt(json.lastIndexOf(","));
            json.append("}");
            returnMap.put(JSONOBJ, enumMap);
            returnMap.put(JSONSTR, json.toString());
        } else {
            returnMap.put(JSONOBJ, enumMap);
            returnMap.put(JSONSTR, "{}");
        }
        return returnMap;
    }


    private static class EnumItem {

        private String value;
        private String label;

        public EnumItem(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
