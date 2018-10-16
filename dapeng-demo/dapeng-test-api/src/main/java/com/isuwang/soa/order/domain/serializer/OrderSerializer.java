 package com.isuwang.soa.order.domain.serializer;
        import com.isuwang.soa.order.domain.serializer.*;import com.github.dapeng.soa.domain.serializer.*;import com.isuwang.soa.price.domain.serializer.*;import com.isuwang.soa.user.domain.serializer.*;import com.isuwang.soa.settle.domain.serializer.*;

        import com.github.dapeng.core.*;
        import com.github.dapeng.org.apache.thrift.*;
        import com.github.dapeng.org.apache.thrift.protocol.*;

        import java.util.Optional;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Future;

        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1-SNAPSHOT)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *  @generated
        **/
        public class OrderSerializer implements BeanSerializer<com.isuwang.soa.order.domain.Order>{
        
      @Override
      public com.isuwang.soa.order.domain.Order read(TProtocol iprot) throws TException{

      com.isuwang.soa.order.domain.Order bean = new com.isuwang.soa.order.domain.Order();
      com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();

      while(true){
        schemeField = iprot.readFieldBegin();
        if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP){ break;}

        switch(schemeField.id){
          
              case 1:
              if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32){
               int elem0 = iprot.readI32();
       bean.setId(elem0);
            }else{
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
              break;
            
              case 2:
              if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING){
              String elem0 = iprot.readString();
       bean.setOrder_no(elem0);
            }else{
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
              break;
            
              case 3:
              if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.I32){
               int elem0 = iprot.readI32();
       bean.setStatus(elem0);
            }else{
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
              break;
            
              case 4:
              if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE){
               double elem0 = iprot.readDouble();
       bean.setAmount(elem0);
            }else{
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
              break;
            
          
            default:
            com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
          
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      validate(bean);
      return bean;
    }
    
      @Override
      public void write(com.isuwang.soa.order.domain.Order bean, TProtocol oprot) throws TException{

      validate(bean);
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("Order"));

      
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("id", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 1));
            Integer elem0 = bean.getId();
            oprot.writeI32(elem0);
            
            oprot.writeFieldEnd();
          
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("order_no", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 2));
            String elem1 = bean.getOrder_no();
            oprot.writeString(elem1);
            
            oprot.writeFieldEnd();
          
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("status", com.github.dapeng.org.apache.thrift.protocol.TType.I32, (short) 3));
            Integer elem2 = bean.getStatus();
            oprot.writeI32(elem2);
            
            oprot.writeFieldEnd();
          
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("amount", com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE, (short) 4));
            Double elem3 = bean.getAmount();
            oprot.writeDouble(elem3);
            
            oprot.writeFieldEnd();
          
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
    
      public void validate(com.isuwang.soa.order.domain.Order bean) throws TException{
      
              if(bean.getOrder_no() == null)
              throw new SoaException(SoaCode.NotNull, "order_no字段不允许为空");
            
    }
    
        @Override
        public String toString(com.isuwang.soa.order.domain.Order bean)
        {return bean == null ? "null" : bean.toString();}
      }
      

      