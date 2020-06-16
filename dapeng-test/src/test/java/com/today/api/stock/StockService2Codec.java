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
package com.today.api.stock;
        import com.today.api.purchase.request.serializer.*;import com.today.api.common.serializer.*;import com.today.api.purchase.response.serializer.*;import com.today.api.stock.response.serializer.*;import com.today.api.stock.request.serializer.*;import com.today.api.stock.events.serializer.*;import com.today.api.stock.vo.serializer.*;

        import com.github.dapeng.core.*;
        import com.github.dapeng.org.apache.thrift.*;
        import com.github.dapeng.org.apache.thrift.protocol.*;

        import com.github.dapeng.core.definition.SoaServiceDefinition;
        import com.github.dapeng.core.definition.SoaFunctionDefinition;

        import java.io.BufferedReader;
        import java.io.InputStreamReader;

        import java.util.Optional;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Future;
        import com.today.api.stock.StockService2SuperCodec.*;

        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1-final)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *
        **/
        public class StockService2Codec {

        
            public static class processStock<I extends com.today.api.stock.service.StockService2> extends SoaFunctionDefinition.Sync<I, processStock_args, processStock_result>
            {
              public processStock()
              {
                super("processStock", new ProcessStock_argsSerializer(),  new ProcessStock_resultSerializer());
              }

              @Override
              public processStock_result apply(I iface, processStock_args processStock_args) throws SoaException
              {
                processStock_result result = new processStock_result();
                
                result.setSuccess(iface.processStock(processStock_args.getEvent()));
                
                return result;
              }

            }
          
            public static class processStockByPiecemeal<I extends com.today.api.stock.service.StockService2> extends SoaFunctionDefinition.Sync<I, processStockByPiecemeal_args, processStockByPiecemeal_result>
            {
              public processStockByPiecemeal()
              {
                super("processStockByPiecemeal", new ProcessStockByPiecemeal_argsSerializer(),  new ProcessStockByPiecemeal_resultSerializer());
              }

              @Override
              public processStockByPiecemeal_result apply(I iface, processStockByPiecemeal_args processStockByPiecemeal_args) throws SoaException
              {
                processStockByPiecemeal_result result = new processStockByPiecemeal_result();
                
                result.setSuccess(iface.processStockByPiecemeal(processStockByPiecemeal_args.getRequest()));
                
                return result;
              }

            }
          

        public static class getServiceMetadata<I extends com.today.api.stock.service.StockService2> extends SoaFunctionDefinition.Sync<I, getServiceMetadata_args, getServiceMetadata_result> {
          public getServiceMetadata() {
            super("getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
          }

          @Override
          public getServiceMetadata_result apply(I iface, getServiceMetadata_args args) {
            getServiceMetadata_result result = new getServiceMetadata_result();

            try (InputStreamReader isr = new InputStreamReader(StockService2Codec.class.getClassLoader().getResourceAsStream("com.today.api.stock.service.StockService2.xml"));
            BufferedReader in = new BufferedReader(isr)) {
              int len = 0;
              StringBuilder str = new StringBuilder("");
              String line;
              while ((line = in.readLine()) != null) {

                if (len != 0) {
                  str.append("\r\n").append(line);
                } else {
                  str.append(line);
                }
                len++;
              }
              result.setSuccess(str.toString());

            } catch (Exception e) {
              e.printStackTrace();
              result.setSuccess("");
            }

            return result;
          }

        }


        public static class echo<I extends com.today.api.stock.service.StockService2> extends SoaFunctionDefinition.Sync<I, echo_args, echo_result> {
          public echo() {
            super("echo", new echo_argsSerializer(), new echo_resultSerializer());
          }

          @Override
          public echo_result apply(I iface, echo_args args) {
            echo_result result = new echo_result();

            String echoMsg = (String) TransactionContext.Factory.currentInstance().getAttribute("container-threadPool-info");
            //result.setSuccess("PONG");
            result.setSuccess(echoMsg);
            return result;

          }

        }


        @SuppressWarnings("unchecked")
        public static class Processor<I extends com.today.api.stock.service.StockService2> extends SoaServiceDefinition<com.today.api.stock.service.StockService2>
        {

          public Processor(com.today.api.stock.service.StockService2 iface, Class<com.today.api.stock.service.StockService2> ifaceClass)
          {
            super(iface, ifaceClass, buildMap(new java.util.HashMap<>()));
          }

          @SuppressWarnings("unchecked")
          private static <I extends com.today.api.stock.service.StockService2> java.util.Map<String, SoaFunctionDefinition<I, ?, ?>> buildMap(java.util.Map<String, SoaFunctionDefinition<I, ?, ?>> processMap)
          {
            
                processMap.put("processStock", new processStock());
              
                processMap.put("processStockByPiecemeal", new processStockByPiecemeal());
              
            processMap.put("getServiceMetadata", new getServiceMetadata());
            processMap.put("echo", new echo());
            return processMap;
          }
        }

      }
      