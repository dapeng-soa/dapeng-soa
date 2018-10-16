package com.github.dapeng.soa.scala
        import com.isuwang.soa.user.scala.domain.serializer._;import com.isuwang.soa.price.scala.domain.serializer._;import com.isuwang.soa.order.scala.domain.serializer._;import com.github.dapeng.soa.scala.domain.serializer._;import com.isuwang.soa.settle.scala.domain.serializer._;

        import com.github.dapeng.core._
        import com.github.dapeng.org.apache.thrift._
        import com.github.dapeng.org.apache.thrift.protocol._
        import com.github.dapeng.core.definition._

        import scala.concurrent.ExecutionContext.Implicits.global
        import java.util.concurrent.{CompletableFuture, Future}
        import scala.util.{Failure, Success}

        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1-SNAPSHOT)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *  @generated
        **/
        object PrintServiceAsyncCodec {

        implicit class FutureX[T](f: scala.concurrent.Future[T]) {
          def tojava(): CompletableFuture[T] = {
            val java = new CompletableFuture[T]()
            f.onComplete{
              case Success(x) => java.complete(x)
              case Failure(ex) => java.completeExceptionally(ex)
            }
            java
          }
        }

        
            case class print_args()

            case class print_result()

            class Print_argsSerializer extends BeanSerializer[print_args]{
            
      @throws[TException]
      override def read(iprot: TProtocol): print_args = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = print_args()
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: print_args, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("print_args"))

      
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: print_args): Unit = {
      
    }
    

            override def toString(bean: print_args): String = if(bean == null)  "null" else bean.toString
          }

            class Print_resultSerializer extends BeanSerializer[print_result]{

            @throws[TException]
            override def read(iprot: TProtocol): print_result = {

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

                schemeField = iprot.readFieldBegin

                schemeField.id match {
                  case 0 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.VOID =>  com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  }
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }

                iprot.readFieldEnd
              }

              iprot.readStructEnd
              val bean = print_result()
              validate(bean)

              bean
            }

            
      @throws[TException]
      override def write(bean: print_result, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("print_result"))

      
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
            
      @throws[TException]
      override def validate(bean: print_result): Unit = {
      
    }
    

            override def toString(bean: print_result): String = if(bean == null)  "null" else bean.toString
          }

            class print extends SoaFunctionDefinition.Async[com.github.dapeng.soa.scala.service.PrintServiceAsync, print_args, print_result]("print", new Print_argsSerializer(), new Print_resultSerializer()){

            @throws[TException]
            def apply(iface: com.github.dapeng.soa.scala.service.PrintServiceAsync, args: print_args):Future[print_result] = {

              val _result = iface.print()

              
                _result.map(i => print_result()).tojava

              

            }
          }
          
            case class printInfo_args(info:com.github.dapeng.soa.scala.domain.Info)

            case class printInfo_result(success:String)

            class PrintInfo_argsSerializer extends BeanSerializer[printInfo_args]{
            
      @throws[TException]
      override def read(iprot: TProtocol): printInfo_args = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      var info: com.github.dapeng.soa.scala.domain.Info = null
        

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
              case 1 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT => info = 
          new com.github.dapeng.soa.scala.domain.serializer.InfoSerializer().read(iprot)
        
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = printInfo_args(info = info)
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: printInfo_args, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("printInfo_args"))

      
            {
            val elem0 = bean.info 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("info", com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT, 1.asInstanceOf[Short]))
            
          new com.github.dapeng.soa.scala.domain.serializer.InfoSerializer().write(elem0, oprot)
        
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: printInfo_args): Unit = {
      
              if(bean.info == null)
              throw new SoaException(SoaCode.NotNull, "info字段不允许为空")
            
                if(bean.info != null)
                new com.github.dapeng.soa.scala.domain.serializer.InfoSerializer().validate(bean.info)
              
    }
    

            override def toString(bean: printInfo_args): String = if(bean == null)  "null" else bean.toString
          }

            class PrintInfo_resultSerializer extends BeanSerializer[printInfo_result]{

            @throws[TException]
            override def read(iprot: TProtocol): printInfo_result = {

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              var success : String = null

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

                schemeField = iprot.readFieldBegin

                schemeField.id match {
                  case 0 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRING =>  success = iprot.readString
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  }
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }

                iprot.readFieldEnd
              }

              iprot.readStructEnd
              val bean = printInfo_result(success)
              validate(bean)

              bean
            }

            
      @throws[TException]
      override def write(bean: printInfo_result, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("printInfo_result"))

      
            {
            val elem0 = bean.success 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oprot.writeString(elem0)
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
            
      @throws[TException]
      override def validate(bean: printInfo_result): Unit = {
      
              if(bean.success == null)
              throw new SoaException(SoaCode.NotNull, "success字段不允许为空")
            
    }
    

            override def toString(bean: printInfo_result): String = if(bean == null)  "null" else bean.toString
          }

            class printInfo extends SoaFunctionDefinition.Async[com.github.dapeng.soa.scala.service.PrintServiceAsync, printInfo_args, printInfo_result]("printInfo", new PrintInfo_argsSerializer(), new PrintInfo_resultSerializer()){

            @throws[TException]
            def apply(iface: com.github.dapeng.soa.scala.service.PrintServiceAsync, args: printInfo_args):Future[printInfo_result] = {

              val _result = iface.printInfo(args.info)

              _result.map(printInfo_result(_)).tojava

            }
          }
          
            case class printInfo2_args(name:String)

            case class printInfo2_result(success:String)

            class PrintInfo2_argsSerializer extends BeanSerializer[printInfo2_args]{
            
      @throws[TException]
      override def read(iprot: TProtocol): printInfo2_args = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      var name: String = null
        

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
              case 1 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRING => name = iprot.readString
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            }
            
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = printInfo2_args(name = name)
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: printInfo2_args, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("printInfo2_args"))

      
            {
            val elem0 = bean.name 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("name", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 1.asInstanceOf[Short]))
            oprot.writeString(elem0)
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: printInfo2_args): Unit = {
      
              if(bean.name == null)
              throw new SoaException(SoaCode.NotNull, "name字段不允许为空")
            
    }
    

            override def toString(bean: printInfo2_args): String = if(bean == null)  "null" else bean.toString
          }

            class PrintInfo2_resultSerializer extends BeanSerializer[printInfo2_result]{

            @throws[TException]
            override def read(iprot: TProtocol): printInfo2_result = {

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              var success : String = null

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

                schemeField = iprot.readFieldBegin

                schemeField.id match {
                  case 0 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRING =>  success = iprot.readString
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  }
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }

                iprot.readFieldEnd
              }

              iprot.readStructEnd
              val bean = printInfo2_result(success)
              validate(bean)

              bean
            }

            
      @throws[TException]
      override def write(bean: printInfo2_result, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("printInfo2_result"))

      
            {
            val elem0 = bean.success 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oprot.writeString(elem0)
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
            
      @throws[TException]
      override def validate(bean: printInfo2_result): Unit = {
      
              if(bean.success == null)
              throw new SoaException(SoaCode.NotNull, "success字段不允许为空")
            
    }
    

            override def toString(bean: printInfo2_result): String = if(bean == null)  "null" else bean.toString
          }

            class printInfo2 extends SoaFunctionDefinition.Async[com.github.dapeng.soa.scala.service.PrintServiceAsync, printInfo2_args, printInfo2_result]("printInfo2", new PrintInfo2_argsSerializer(), new PrintInfo2_resultSerializer()){

            @throws[TException]
            def apply(iface: com.github.dapeng.soa.scala.service.PrintServiceAsync, args: printInfo2_args):Future[printInfo2_result] = {

              val _result = iface.printInfo2(args.name)

              _result.map(printInfo2_result(_)).tojava

            }
          }
          
            case class printInfo3_args()

            case class printInfo3_result(success:String)

            class PrintInfo3_argsSerializer extends BeanSerializer[printInfo3_args]{
            
      @throws[TException]
      override def read(iprot: TProtocol): printInfo3_args = {

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

        schemeField = iprot.readFieldBegin

        schemeField.id match {
          
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        }
      }

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = printInfo3_args()
      validate(bean)

      bean
      }
    
      @throws[TException]
      override def write(bean: printInfo3_args, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("printInfo3_args"))

      
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
      @throws[TException]
      override def validate(bean: printInfo3_args): Unit = {
      
    }
    

            override def toString(bean: printInfo3_args): String = if(bean == null)  "null" else bean.toString
          }

            class PrintInfo3_resultSerializer extends BeanSerializer[printInfo3_result]{

            @throws[TException]
            override def read(iprot: TProtocol): printInfo3_result = {

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              var success : String = null

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {

                schemeField = iprot.readFieldBegin

                schemeField.id match {
                  case 0 =>
                  schemeField.`type` match {
                    case com.github.dapeng.org.apache.thrift.protocol.TType.STRING =>  success = iprot.readString
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  }
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }

                iprot.readFieldEnd
              }

              iprot.readStructEnd
              val bean = printInfo3_result(success)
              validate(bean)

              bean
            }

            
      @throws[TException]
      override def write(bean: printInfo3_result, oprot: TProtocol): Unit = {

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("printInfo3_result"))

      
            {
            val elem0 = bean.success 
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oprot.writeString(elem0)
            oprot.writeFieldEnd
            
            }
      oprot.writeFieldStop
      oprot.writeStructEnd
    }
    
            
      @throws[TException]
      override def validate(bean: printInfo3_result): Unit = {
      
              if(bean.success == null)
              throw new SoaException(SoaCode.NotNull, "success字段不允许为空")
            
    }
    

            override def toString(bean: printInfo3_result): String = if(bean == null)  "null" else bean.toString
          }

            class printInfo3 extends SoaFunctionDefinition.Async[com.github.dapeng.soa.scala.service.PrintServiceAsync, printInfo3_args, printInfo3_result]("printInfo3", new PrintInfo3_argsSerializer(), new PrintInfo3_resultSerializer()){

            @throws[TException]
            def apply(iface: com.github.dapeng.soa.scala.service.PrintServiceAsync, args: printInfo3_args):Future[printInfo3_result] = {

              val _result = iface.printInfo3()

              _result.map(printInfo3_result(_)).tojava

            }
          }
          

        case class getServiceMetadata_args()

        case class getServiceMetadata_result(success: String)

        class GetServiceMetadata_argsSerializer extends BeanSerializer[getServiceMetadata_args] {

          @throws[TException]
          override def read(iprot: TProtocol): getServiceMetadata_args = {

            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
              schemeField = iprot.readFieldBegin
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              iprot.readFieldEnd
            }

            iprot.readStructEnd

            val bean = getServiceMetadata_args()
            validate(bean)

            bean
          }

          @throws[TException]
          override def write(bean: getServiceMetadata_args, oproto: TProtocol): Unit = {
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_args"))

            oproto.writeFieldStop
            oproto.writeStructEnd
          }

          @throws[TException]
          override def validate(bean: getServiceMetadata_args): Unit = {}

          override def toString(bean: getServiceMetadata_args): String = if (bean == null) "null" else bean.toString
        }



        class GetServiceMetadata_resultSerializer extends BeanSerializer[getServiceMetadata_result] {
          @throws[TException]
          override def read(iprot: TProtocol): getServiceMetadata_result = {
            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            var success: String = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) {
              schemeField = iprot.readFieldBegin

              schemeField.id match {
                case 0 =>
                schemeField.`type` match {
                  case com.github.dapeng.org.apache.thrift.protocol.TType.STRING => success = iprot.readString
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                }
                case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              }
              iprot.readFieldEnd
            }

            iprot.readStructEnd
            val bean = getServiceMetadata_result(success)
            validate(bean)

            bean
          }

          @throws[TException]
          override def write(bean: getServiceMetadata_result, oproto: TProtocol): Unit = {
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_result"))

            oproto.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oproto.writeString(bean.success)
            oproto.writeFieldEnd

            oproto.writeFieldStop
            oproto.writeStructEnd
          }

          @throws[TException]
          override def validate(bean: getServiceMetadata_result): Unit = {
            if (bean.success == null)
            throw new SoaException(SoaCode.NotNull, "success字段不允许为空")
          }

          override def toString(bean: getServiceMetadata_result): String = if (bean == null) "null" else bean.toString

        }



        class getServiceMetadata extends SoaFunctionDefinition.Async[com.github.dapeng.soa.scala.service.PrintServiceAsync, getServiceMetadata_args, getServiceMetadata_result](
        "getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer()) {


          @throws[TException]
          override def apply(iface: com.github.dapeng.soa.scala.service.PrintServiceAsync, args: getServiceMetadata_args): Future[getServiceMetadata_result] = {

            val result = scala.concurrent.Future {
            val source = scala.io.Source.fromInputStream(PrintServiceCodec.getClass.getClassLoader.getResourceAsStream("com.github.dapeng.soa.service.PrintService.xml"))
            val success = source.mkString
            source.close
            getServiceMetadata_result(success)
            }
            result.tojava

          }
        }

        class Processor(iface: com.github.dapeng.soa.scala.service.PrintServiceAsync, ifaceClass: Class[com.github.dapeng.soa.scala.service.PrintServiceAsync]) extends
        SoaServiceDefinition(iface,classOf[com.github.dapeng.soa.scala.service.PrintServiceAsync], Processor.buildMap)

        object Processor{

          type PF = SoaFunctionDefinition[com.github.dapeng.soa.scala.service.PrintServiceAsync, _, _]

          def buildMap(): java.util.Map[String, PF] = {
            val map = new java.util.HashMap[String, PF]()
            map.put("print", new print)
              map.put("printInfo", new printInfo)
              map.put("printInfo2", new printInfo2)
              map.put("printInfo3", new printInfo3)
              
            map.put("getServiceMetadata", new getServiceMetadata)
            map
          }

        }
      }
      