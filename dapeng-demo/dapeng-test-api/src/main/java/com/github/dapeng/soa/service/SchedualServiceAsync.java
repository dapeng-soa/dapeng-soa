
        package com.github.dapeng.soa.service;

        import com.github.dapeng.core.Processor;
        import com.github.dapeng.core.Service;
        import com.github.dapeng.core.SoaGlobalTransactional;

        import java.util.concurrent.Future;

        /**
         * Autogenerated by Dapeng-Code-Generator (1.2.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

        * 
        **/
        @Service(name="com.github.dapeng.soa.service.SchedualService",version = "1.0.0")
        @Processor(className = "com.github.dapeng.soa.SchedualServiceAsyncCodec$Processor")
        public interface SchedualServiceAsync  extends com.github.dapeng.core.definition.AsyncService {
        
            /**
            * 
            **/
            
            
              Future<String> test(  long timeout) throws com.github.dapeng.core.SoaException;
            
          
      }
      