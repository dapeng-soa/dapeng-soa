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
package com.today.api.purchase.request;

        import java.util.Optional;
        import com.github.dapeng.org.apache.thrift.TException;
        import com.github.dapeng.org.apache.thrift.protocol.TCompactProtocol;
        import com.github.dapeng.util.TCommonTransport;

        /**
         * Autogenerated by Dapeng-Code-Generator (2.1.1-final)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING

        *
        **/
        public class CreateFFStockRequest{
        
            /**
            *
            **/
            public java.util.List<FFStockVo> ffStockList = new java.util.ArrayList();
            public java.util.List<FFStockVo> getFfStockList(){ return this.ffStockList; }
            public void setFfStockList(java.util.List<FFStockVo> ffStockList){ this.ffStockList = ffStockList; }

            public java.util.List<FFStockVo> ffStockList(){ return this.ffStockList; }
            public CreateFFStockRequest ffStockList(java.util.List<FFStockVo> ffStockList){ this.ffStockList = ffStockList; return this; }
          

        public static byte[] getBytesFromBean(CreateFFStockRequest bean) throws TException {
          byte[] bytes = new byte[]{};
          TCommonTransport transport = new TCommonTransport(bytes, TCommonTransport.Type.Write);
          TCompactProtocol protocol = new TCompactProtocol(transport);

          new com.today.api.purchase.request.serializer.CreateFFStockRequestSerializer().write(bean, protocol);
          transport.flush();
          return transport.getByteBuf();
        }

        public static CreateFFStockRequest getBeanFromBytes(byte[] bytes) throws TException {
          TCommonTransport transport = new TCommonTransport(bytes, TCommonTransport.Type.Read);
          TCompactProtocol protocol = new TCompactProtocol(transport);
          return new com.today.api.purchase.request.serializer.CreateFFStockRequestSerializer().read(protocol);
        }

        public String toString(){
          StringBuilder stringBuilder = new StringBuilder("{");
            stringBuilder.append("\"").append("ffStockList").append("\":").append(this.ffStockList).append(",");
    
            stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
            stringBuilder.append("}");

          return stringBuilder.toString();
        }
      }
      