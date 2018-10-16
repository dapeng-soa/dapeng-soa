package com.isuwang.soa.price;
        import com.isuwang.soa.order.domain.serializer.*;import com.github.dapeng.soa.domain.serializer.*;import com.isuwang.soa.price.domain.serializer.*;import com.isuwang.soa.user.domain.serializer.*;import com.isuwang.soa.settle.domain.serializer.*;

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

        import com.isuwang.soa.price.service.PriceServiceAsync;
        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1-SNAPSHOT)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *  @generated
        **/
        public class PriceServiceAsyncCodec {

        
            public static class insertPrice_args {
            
                private com.isuwang.soa.price.domain.Price price;
                public com.isuwang.soa.price.domain.Price getPrice(){
                return this.price;
              }
                public void setPrice(com.isuwang.soa.price.domain.Price price){
                this.price = price;
              }
              

            @Override
            public String toString(){
              StringBuilder stringBuilder = new StringBuilder("{");
                
      stringBuilder.append("\"").append("price").append("\":").append(this.price == null ? "null" : this.price.toString()).append(",");
    
                if(stringBuilder.lastIndexOf(",") > 0)
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                stringBuilder.append("}");

              return stringBuilder.toString();
            }

          }

            public static class insertPrice_result {

            
                  @Override
                  public String toString(){
                  return "{}";
                }
                
          }

            public static class InsertPrice_argsSerializer implements BeanSerializer<insertPrice_args>{
            
      @Override
      public insertPrice_args read(TProtocol iprot) throws TException{

      insertPrice_args bean = new insertPrice_args();
      com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();

      while(true){
        schemeField = iprot.readFieldBegin();
        if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP){ break;}

        switch(schemeField.id){
          
              case 1:
              if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT){
              com.isuwang.soa.price.domain.Price elem0 = new com.isuwang.soa.price.domain.Price();
        elem0=new com.isuwang.soa.price.domain.serializer.PriceSerializer().read(iprot);
       bean.setPrice(elem0);
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
      public void write(insertPrice_args bean, TProtocol oprot) throws TException{

      validate(bean);
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("insertPrice_args"));

      
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("price", com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, (short) 1));
            com.isuwang.soa.price.domain.Price elem0 = bean.getPrice();
             new com.isuwang.soa.price.domain.serializer.PriceSerializer().write(elem0, oprot);
            
            oprot.writeFieldEnd();
          
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
    
      public void validate(insertPrice_args bean) throws TException{
      
              if(bean.getPrice() == null)
              throw new SoaException(SoaCode.NotNull, "price字段不允许为空");
            
                if(bean.getPrice() != null)
                new com.isuwang.soa.price.domain.serializer.PriceSerializer().validate(bean.getPrice());
              
    }
    

            @Override
            public String toString(insertPrice_args bean) { return bean == null ? "null" : bean.toString(); }

          }

            public static class InsertPrice_resultSerializer implements BeanSerializer<insertPrice_result>{
            @Override
            public insertPrice_result read(TProtocol iprot) throws TException{

              insertPrice_result bean = new insertPrice_result();
              com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
              iprot.readStructBegin();

              while(true){
                schemeField = iprot.readFieldBegin();
                if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP){ break;}

                switch(schemeField.id){
                  case 0:  //SUCCESS
                  if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.VOID){
                    
      com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                  }else{
                    com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                  }
                  break;
                  /*
                  case 1: //ERROR
                  bean.setSoaException(new SoaException());
                  new SoaExceptionSerializer().read(bean.getSoaException(),iprot);
                  break A;
                  */
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
      public void write(insertPrice_result bean, TProtocol oprot) throws TException{

      validate(bean);
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("insertPrice_result"));

      
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
    
            
      public void validate(insertPrice_result bean) throws TException{
      
    }
    

            @Override
            public String toString(insertPrice_result bean) { return bean == null ? "null" : bean.toString(); }
          }

            public static class insertPrice<I extends com.isuwang.soa.price.service.PriceServiceAsync> extends SoaFunctionDefinition.Async<I, insertPrice_args, insertPrice_result>
            {
              public insertPrice()
              {
                super("insertPrice", new InsertPrice_argsSerializer(),  new InsertPrice_resultSerializer());
              }

              @Override
              public CompletableFuture<insertPrice_result> apply(PriceServiceAsync iface, insertPrice_args insertPrice_args) throws SoaException
              {

                CompletableFuture<Void> result = (CompletableFuture<Void>) iface.insertPrice(insertPrice_args.price);

                return result.thenApply((Void) -> {
                  insertPrice_result res = new insertPrice_result();
                  
                  return res;
              });
              }

            }
          
            public static class getPrices_args {
            

            @Override
            public String toString(){
              StringBuilder stringBuilder = new StringBuilder("{");
                
                if(stringBuilder.lastIndexOf(",") > 0)
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                stringBuilder.append("}");

              return stringBuilder.toString();
            }

          }

            public static class getPrices_result {

            
                  private java.util.List<com.isuwang.soa.price.domain.Price> success;
                  public java.util.List<com.isuwang.soa.price.domain.Price> getSuccess(){
                  return success;
                }

                  public void setSuccess(java.util.List<com.isuwang.soa.price.domain.Price> success){
                  this.success = success;
                }


                  @Override
                  public String toString(){
                  StringBuilder stringBuilder = new StringBuilder("{");
                    
      stringBuilder.append("\"").append("success").append("\":").append(success).append(",");
    
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                    stringBuilder.append("}");

                  return stringBuilder.toString();
                }

                
          }

            public static class GetPrices_argsSerializer implements BeanSerializer<getPrices_args>{
            
      @Override
      public getPrices_args read(TProtocol iprot) throws TException{

      getPrices_args bean = new getPrices_args();
      com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();

      while(true){
        schemeField = iprot.readFieldBegin();
        if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP){ break;}

        switch(schemeField.id){
          
          
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
      public void write(getPrices_args bean, TProtocol oprot) throws TException{

      validate(bean);
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getPrices_args"));

      
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
    
      public void validate(getPrices_args bean) throws TException{
      
    }
    

            @Override
            public String toString(getPrices_args bean) { return bean == null ? "null" : bean.toString(); }

          }

            public static class GetPrices_resultSerializer implements BeanSerializer<getPrices_result>{
            @Override
            public getPrices_result read(TProtocol iprot) throws TException{

              getPrices_result bean = new getPrices_result();
              com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
              iprot.readStructBegin();

              while(true){
                schemeField = iprot.readFieldBegin();
                if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP){ break;}

                switch(schemeField.id){
                  case 0:  //SUCCESS
                  if(schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.LIST){
                     com.github.dapeng.org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
        java.util.List<com.isuwang.soa.price.domain.Price> elem0 = new java.util.ArrayList<>(_list0.size);
        for(int _i0 = 0; _i0 < _list0.size; ++ _i0){
          com.isuwang.soa.price.domain.Price elem1 = new com.isuwang.soa.price.domain.Price();
        elem1=new com.isuwang.soa.price.domain.serializer.PriceSerializer().read(iprot);
          elem0.add(elem1);
        }
        iprot.readListEnd();
       bean.setSuccess(elem0);
                  }else{
                    com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                  }
                  break;
                  /*
                  case 1: //ERROR
                  bean.setSoaException(new SoaException());
                  new SoaExceptionSerializer().read(bean.getSoaException(),iprot);
                  break A;
                  */
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
      public void write(getPrices_result bean, TProtocol oprot) throws TException{

      validate(bean);
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getPrices_result"));

      
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.LIST, (short) 0));
            java.util.List<com.isuwang.soa.price.domain.Price> elem0 = bean.getSuccess();
            
          oprot.writeListBegin(new com.github.dapeng.org.apache.thrift.protocol.TList(com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, elem0.size()));
          for(com.isuwang.soa.price.domain.Price elem1 : elem0){
           new com.isuwang.soa.price.domain.serializer.PriceSerializer().write(elem1, oprot);
        }
          oprot.writeListEnd();
        
            
            oprot.writeFieldEnd();
          
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
    
            
      public void validate(getPrices_result bean) throws TException{
      
              if(bean.getSuccess() == null)
              throw new SoaException(SoaCode.NotNull, "success字段不允许为空");
            
    }
    

            @Override
            public String toString(getPrices_result bean) { return bean == null ? "null" : bean.toString(); }
          }

            public static class getPrices<I extends com.isuwang.soa.price.service.PriceServiceAsync> extends SoaFunctionDefinition.Async<I, getPrices_args, getPrices_result>
            {
              public getPrices()
              {
                super("getPrices", new GetPrices_argsSerializer(),  new GetPrices_resultSerializer());
              }

              @Override
              public CompletableFuture<getPrices_result> apply(PriceServiceAsync iface, getPrices_args getPrices_args) throws SoaException
              {

                CompletableFuture<java.util.List<com.isuwang.soa.price.domain.Price>> result = (CompletableFuture<java.util.List<com.isuwang.soa.price.domain.Price>>) iface.getPrices();

                return result.thenApply(( java.util.List<com.isuwang.soa.price.domain.Price> i) -> {
                  getPrices_result res = new getPrices_result();
                  res.setSuccess(i);
                  return res;
              });
              }

            }
          

        public static class getServiceMetadata_args {

          @Override
          public String toString() {
            return "{}";
          }
        }


        public static class getServiceMetadata_result {

          private String success;

          public String getSuccess() {
            return success;
          }

          public void setSuccess(String success) {
            this.success = success;
          }

          @Override
          public String toString() {
            StringBuilder stringBuilder = new StringBuilder("{");
              stringBuilder.append("\"").append("success").append("\":\"").append(this.success).append("\",");
              stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
              stringBuilder.append("}");

            return stringBuilder.toString();
          }
        }

        public static class GetServiceMetadata_argsSerializer implements BeanSerializer<getServiceMetadata_args> {

          @Override
          public getServiceMetadata_args read(TProtocol iprot) throws TException {

            getServiceMetadata_args bean =new getServiceMetadata_args();
            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
              schemeField = iprot.readFieldBegin();
              if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                break;
              }
              switch (schemeField.id) {
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
          public void write(getServiceMetadata_args bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_args"));
            oprot.writeFieldStop();
            oprot.writeStructEnd();
          }

          public void validate(getServiceMetadata_args bean) throws TException {}

          @Override
          public String toString(getServiceMetadata_args bean) {
            return bean == null ? "null" : bean.toString();
          }

        }

        public static class GetServiceMetadata_resultSerializer implements BeanSerializer<getServiceMetadata_result> {
          @Override
          public getServiceMetadata_result read(TProtocol iprot) throws TException {

            getServiceMetadata_result bean = new getServiceMetadata_result();
            com.github.dapeng.org.apache.thrift.protocol.TField schemeField;
            iprot.readStructBegin();

            while (true) {
              schemeField = iprot.readFieldBegin();
              if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
                break;
              }

              switch (schemeField.id) {
                case 0:  //SUCCESS
                if (schemeField.type == com.github.dapeng.org.apache.thrift.protocol.TType.STRING) {
                  bean.setSuccess(iprot.readString());
                } else {
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
          public void write(getServiceMetadata_result bean, TProtocol oprot) throws TException {

            validate(bean);
            oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_result"));

            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, (short) 0));
            oprot.writeString(bean.getSuccess());
            oprot.writeFieldEnd();

            oprot.writeFieldStop();
            oprot.writeStructEnd();
          }

          public void validate(getServiceMetadata_result bean) throws TException {

            if (bean.getSuccess() == null)
            throw new SoaException(SoaCode.NotNull, "success字段不允许为空");
          }

          @Override
          public String toString(getServiceMetadata_result bean) {
            return bean == null ? "null" : bean.toString();
          }
        }

        public static class getServiceMetadata<I extends com.isuwang.soa.price.service.PriceServiceAsync> extends SoaFunctionDefinition.Async<I, getServiceMetadata_args, getServiceMetadata_result> {
          public getServiceMetadata() {
            super("getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer());
          }

          @Override
          public CompletableFuture<getServiceMetadata_result> apply(I iface, getServiceMetadata_args args) {
            getServiceMetadata_result result = new getServiceMetadata_result();

           return CompletableFuture.supplyAsync(() -> {
            try (InputStreamReader isr = new InputStreamReader(PriceServiceCodec.class.getClassLoader().getResourceAsStream("com.isuwang.soa.price.service.PriceService.xml"));
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
              result.success = str.toString();

            } catch (Exception e) {
              e.printStackTrace();
              result.success = "";
            }

            return result;
          });
          }

        }

        @SuppressWarnings("unchecked")
        public static class Processor<I extends com.isuwang.soa.price.service.PriceServiceAsync> extends SoaServiceDefinition<com.isuwang.soa.price.service.PriceServiceAsync>
        {

          public Processor(com.isuwang.soa.price.service.PriceServiceAsync iface, Class<com.isuwang.soa.price.service.PriceServiceAsync> ifaceClass)
          {
            super(iface, ifaceClass, buildMap(new java.util.HashMap<>()));
          }

          @SuppressWarnings("unchecked")
          private static <I extends com.isuwang.soa.price.service.PriceServiceAsync> java.util.Map<String, SoaFunctionDefinition<I, ?, ?>> buildMap(java.util.Map<String, SoaFunctionDefinition<I, ?, ?>> processMap)
          {
            
                processMap.put("insertPrice", new insertPrice());
              
                processMap.put("getPrices", new getPrices());
              
            processMap.put("getServiceMetadata", new getServiceMetadata());
            return processMap;
          }
        }

      }
      