
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
package com.today.api.stock.service;

        import com.github.dapeng.core.Processor;
        import com.github.dapeng.core.Service;
        import com.github.dapeng.core.SoaGlobalTransactional;

        import java.util.concurrent.Future;

        /**
         * Autogenerated by Dapeng-Code-Generator (2.2.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING

        * 

 定时任务

        **/
        @com.github.dapeng.core.CustomConfig()
        @Service(name="com.today.api.stock.service.StockScheduledService",version = "1.0.0")
        @Processor(className = "com.today.api.stock.StockScheduledServiceAsyncCodec$Processor")
        public interface StockScheduledServiceAsync  extends com.github.dapeng.core.definition.AsyncService {
        
            /**
            * 

# 库存定时任务服务
## 接口依赖
    无
## 注意事项
    1.库存定时任务服务
    2.一般不需在文档站点测试


# 门店单品推移表统计跑批
## 业务描述
    统计门店中每个sku一天中的库存变化情况
## 接口依赖
    无
## 边界异常说明
    无
## 输入
    无
## 前置检查
    无
##  权限检查
    无
##  逻辑处理
    维度：店铺，sku_no,当天数据,数量
     1.期初  获取前一天的期末值 没有默认为0
     2.进/转入 当天库存流水中转进的数据
     3.退/转出 当天库存流水中转出的数据
     4.报废 当天库存流水中报废的数据
     5.销售 当天库存流水中销售的数据
     6.盘盈亏 当天库存流水中盘盈亏的数据
     7.期末
     8.库存异动值

## 数据库变更
    1. insert sku_stock_summary
##  事务处理
    无
##  输出
    无


# 盘点后 修改实际库存 缠身盘点流水
## 接口依赖
        无
## 注意事项
        1.库存定时任务服务
        2.一般不需在文档站点测试


# 门店单品推移表统计跑批
## 业务描述
        统计门店中每个sku一天中的库存变化情况
## 接口依赖
        无
## 边界异常说明
        无
## 输入
        无
## 前置检查
        无
##  权限检查
        无
##  逻辑处理
        维度：店铺，sku_no,当天数据,数量，价格
         1.期初  获取前一天的期末值 没有默认为0
         2.进/转入 当天库存流水中转进的数据
         3.退/转出 当天库存流水中转出的数据
         4.报废 当天库存流水中报废的数据
         5.销售 当天库存流水中销售的数据
         6.盘盈亏 当天库存流水中盘盈亏的数据
         7.期末
         8.库存异动值

## 数据库变更
        1. insert store_stock_summary
##  事务处理
        无
##  输出
        无


# 同步实时库存到饿了么门店
## 接口依赖
            无
## 注意事项
            1.库存定时任务服务
            2.一般不需在文档站点测试


# 同步实时库存到饿了么门店
## 业务描述
            根据饿了么中的shop_id和custom_sku_id,同步实时库存
## 接口依赖
            无
## 边界异常说明
            无
## 输入
            无
## 前置检查
            无
##  权限检查
            无
##  逻辑处理
            维度：店铺，sku_no,stock_num

             1.饿了么门店根据elemCode和财务店号一一对应
             2.饿了么custom_sku_id与sku_no相对应
             3.根据storeId，skuNo确定实时库存

## 数据库变更
            无
##  事务处理
            无
##  输出
            无

            **/
            
            
              
              Future<Void> autoCalculateElemStock() throws com.github.dapeng.core.SoaException;
            
          
      }
      