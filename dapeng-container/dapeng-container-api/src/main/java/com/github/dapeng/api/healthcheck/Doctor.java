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
package com.github.dapeng.api.healthcheck;

import com.github.dapeng.core.enums.ServiceHealthStatus;

import java.util.Map;

/**
 * @author ever
 * @date 2018/07/26
 */
public interface Doctor {


    /**
     * 上层业务汇报接口
     *
     * @param serviceClass 服务实现类
     * @param status       状态
     * @param remark       具体的信息, 例如: 三方接口xxx不可用
     */
    void report(ServiceHealthStatus status, String remark, Class<?> serviceClass);

    /**
     * 返回服务的健康度, Json格式
     * <pre>
     *  {
     *     "services": [
     *           {
     *                "service": "com.xx.xx.OrderService",
     *                "status": "Yellow", //如果是Yellow/Red, 需带上备注信息.
     *                "remarks": "三方接口xxx不可用"
     *           },
     *           {
     *                "service": "com.xx.xx.OrderScheduleService",
     *                "status": "Red", //如果是Yellow/Red, 需带上备注信息.
     *                "remarks": "定时任务xxx失败!"
     *           }
     *     ],
     *     "tasks": { //当前排队请求数/成功返回请求数/总请求数
     *         "waitingQueue": 10,
     *         "succeed": 1580,
     *         "total": 1590
     *     },
     *     "errors": { //当天系统异常/当天总异常
     *         "system": 2,
     *         "total": 10
     *     },
     *     "flows": { //当天最大流量/最小流量/平均流量(分钟为单位)
     *         "max": 3508,
     *         "min": 280,
     *         "mid": 1280
     *     },
     *     "gcInfos": "2048/0",     //minorGc, majorGc
     *  }
     *  </pre>
     *
     * @return
     */
    Map<String, Object> diagnoseReport();
}
