package com.github.dapeng.code.generator

import java.util
import java.util.stream.Collectors

import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata._


import scala.xml.Elem

/**
 * JAVA生成器
 *
 * @author tangliu
 * @date 15/9/8
 */
class ScalaCodecGenerator extends CodeGenerator {

  override def generate(services: util.List[Service], outDir: String, generateAll:Boolean , structs: util.List[Struct], enums:util.List[TEnum]): Unit = {}

  override def generate(services: util.List[Service], outDir: String): Unit = {

  }

  val keywords = Set("type") // TODO is there any other keyword need to be escape
  def nameAsId(name: String) = if(keywords contains name) s"`$name`" else name

  def toSuperCodecTemplate(service:Service,structNamespaces:util.Set[String], oriNamespace: String): Elem = {
    return {
      <div>package {service.namespace.substring(0, service.namespace.lastIndexOf("."))}
        {
        import collection.JavaConversions._
        structNamespaces.map(struct => {
          <div>import {struct}.serializer._;</div>
        })
        }

        import com.github.dapeng.core._;
        import com.github.dapeng.org.apache.thrift._;
        import com.github.dapeng.org.apache.thrift.protocol._;

        object {service.name}SuperCodec <block>
            //1.
        {
        toMethodArrayBuffer(service.methods).map{(method: Method)=> {
          <div>
            case class {method.name}_args({toFieldArrayBuffer(method.request.getFields).map{(field: Field)=>{<div>{nameAsId(field.name)}:{toScalaDataType(field.dataType)}{if(field != method.request.getFields.get(method.request.getFields.size-1)) <span>,</span>}</div>}}})

            case class {method.name}_result({toFieldArrayBuffer(method.response.getFields).map{(field: Field)=>{<div>{caseClassFiledCombile(field)}{if(field != method.response.getFields.get(method.response.getFields.size-1)) <span>,</span>}</div>}}})

            class {method.name.charAt(0).toUpper + method.name.substring(1)}_argsSerializer extends BeanSerializer[{method.name}_args]<block>
            {getReadMethod(method.getRequest)}{getWriteMethod(method.getRequest)}{getValidateMethod(method.getRequest)}

            override def toString(bean: {method.name}_args): String = if(bean == null)  "null" else bean.toString
          </block>

            class {method.name.charAt(0).toUpper + method.name.substring(1)}_resultSerializer extends BeanSerializer[{method.name}_result]<block>

            @throws[TException]
            override def read(iprot: TProtocol): {method.name}_result = <block>

              var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
              iprot.readStructBegin

              {if(method.response.fields.get(0).dataType.kind != KIND.VOID) <div>var success : {toScalaDataType(method.response.fields.get(0).dataType)} = {getDefaultValueWithType(method.response.fields.get(0).dataType)}</div>}

              while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) <block>

                schemeField = iprot.readFieldBegin

                schemeField.id match <block>
                  case 0 =>
                  schemeField.`type` match <block>
                    case {toThriftDateType(method.response.fields.get(0).dataType)} =>  {if(method.response.fields.get(0).dataType.kind != KIND.VOID) <div>success = {getScalaReadElement(method.response.fields.get(0).dataType, 0)}</div> else <div>com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)</div>}
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                  </block>
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                </block>

                iprot.readFieldEnd
              </block>

              iprot.readStructEnd
              val bean = {method.name}_result({if(method.response.fields.get(0).dataType.kind != KIND.VOID) <div>success</div>})
              validate(bean)

              bean
            </block>

            {getWriteMethod(method.getResponse)}
            {getValidateMethod(method.getResponse)}

            override def toString(bean: {method.name}_result): String = if(bean == null)  "null" else bean.toString
          </block>
            </div>
        }}
        }

        case class getServiceMetadata_args()

        case class getServiceMetadata_result(success: String)

        class GetServiceMetadata_argsSerializer extends BeanSerializer[getServiceMetadata_args] <block>

          @throws[TException]
          override def read(iprot: TProtocol): getServiceMetadata_args = <block>

            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) <block>
              schemeField = iprot.readFieldBegin
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              iprot.readFieldEnd
            </block>

            iprot.readStructEnd

            val bean = getServiceMetadata_args()
            validate(bean)

            bean
          </block>

          @throws[TException]
          override def write(bean: getServiceMetadata_args, oproto: TProtocol): Unit = <block>
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_args"))

            oproto.writeFieldStop
            oproto.writeStructEnd
          </block>

          @throws[TException]
          override def validate(bean: getServiceMetadata_args): Unit = <block></block>

          override def toString(bean: getServiceMetadata_args): String = if (bean == null) "null" else bean.toString
        </block>



        class GetServiceMetadata_resultSerializer extends BeanSerializer[getServiceMetadata_result] <block>
          @throws[TException]
          override def read(iprot: TProtocol): getServiceMetadata_result = <block>
            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            var success: String = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) <block>
              schemeField = iprot.readFieldBegin

              schemeField.id match <block>
                case 0 =>
                schemeField.`type` match <block>
                  case com.github.dapeng.org.apache.thrift.protocol.TType.STRING => success = iprot.readString
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                </block>
                case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              </block>
              iprot.readFieldEnd
            </block>

            iprot.readStructEnd
            val bean = getServiceMetadata_result(success)
            validate(bean)

            bean
          </block>

          @throws[TException]
          override def write(bean: getServiceMetadata_result, oproto: TProtocol): Unit = <block>
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("getServiceMetadata_result"))

            oproto.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oproto.writeString(bean.success)
            oproto.writeFieldEnd

            oproto.writeFieldStop
            oproto.writeStructEnd
          </block>

          @throws[TException]
          override def validate(bean: getServiceMetadata_result): Unit = <block>
            if (bean.success == null)
            throw new SoaException(SoaCode.StructFieldNull, "success字段不允许为空")
          </block>

          override def toString(bean: getServiceMetadata_result): String = if (bean == null) "null" else bean.toString

        </block>


        case class echo_args()

        case class echo_result(success: String)

        class echo_argsSerializer extends BeanSerializer[echo_args] <block>

          @throws[TException]
          override def read(iprot: TProtocol): echo_args = <block>

            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) <block>
              schemeField = iprot.readFieldBegin
              com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              iprot.readFieldEnd
            </block>

            iprot.readStructEnd

            val bean = echo_args()
            validate(bean)

            bean
          </block>

          @throws[TException]
          override def write(bean: echo_args, oproto: TProtocol): Unit = <block>
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("echo_args"))

            oproto.writeFieldStop
            oproto.writeStructEnd
          </block>

          @throws[TException]
          override def validate(bean: echo_args): Unit = <block></block>

          override def toString(bean: echo_args): String = if (bean == null) "null" else bean.toString
        </block>



        class echo_resultSerializer extends BeanSerializer[echo_result] <block>
          @throws[TException]
          override def read(iprot: TProtocol): echo_result = <block>
            iprot.readStructBegin

            var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null

            var success: String = null

            while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) <block>
              schemeField = iprot.readFieldBegin

              schemeField.id match <block>
                case 0 =>
                schemeField.`type` match <block>
                  case com.github.dapeng.org.apache.thrift.protocol.TType.STRING => success = iprot.readString
                  case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
                </block>
                case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
              </block>
              iprot.readFieldEnd
            </block>

            iprot.readStructEnd
            val bean = echo_result(success)
            validate(bean)

            bean
          </block>

          @throws[TException]
          override def write(bean: echo_result, oproto: TProtocol): Unit = <block>
            validate(bean)
            oproto.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("echo_result"))

            oproto.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("success", com.github.dapeng.org.apache.thrift.protocol.TType.STRING, 0.asInstanceOf[Short]))
            oproto.writeString(bean.success)
            oproto.writeFieldEnd

            oproto.writeFieldStop
            oproto.writeStructEnd
          </block>

          @throws[TException]
          override def validate(bean: echo_result): Unit = <block>
            if (bean.success == null)
            throw new SoaException(SoaCode.RespFieldNull, "success字段不允许为空")
          </block>

          override def toString(bean: echo_result): String = if (bean == null) "null" else bean.toString

        </block>



      </block>

      </div>
    }
  }

  def toCodecTemplate(service:Service,structNamespaces:util.Set[String], oriNamespace: String): Elem = {
    //val structNameCache = new util.ArrayList[String]()

    return {
      <div>package {service.namespace.substring(0, service.namespace.lastIndexOf("."))}
        {
          import collection.JavaConversions._
          structNamespaces.map(struct => {
            <div>import {struct}.serializer._;</div>
          })
        }

        import com.github.dapeng.core._
        import com.github.dapeng.org.apache.thrift._
        import com.github.dapeng.org.apache.thrift.protocol._
        import com.github.dapeng.core.definition._
        import {service.namespace.substring(0, service.namespace.lastIndexOf("."))}.{service.name}SuperCodec._

        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *
        **/
        object {service.name}Codec <block>

        {toMethodArrayBuffer(service.methods).map{(method: Method)=> {


          <div>
            class {method.name} extends SoaFunctionDefinition.Sync[{service.namespace}.{service.name}, {method.name}_args, {method.name}_result]("{method.name}", new {method.name.charAt(0).toUpper + method.name.substring(1)}_argsSerializer(), new {method.name.charAt(0).toUpper + method.name.substring(1)}_resultSerializer())<block>

            @throws[TException]
            def apply(iface: {service.namespace}.{service.name}, args: {method.name}_args):{method.name}_result = <block>

              val _result = iface.{method.name}({toFieldArrayBuffer(method.request.getFields).map{(field: Field)=>{<div>args.{nameAsId(field.name)}{if(field != method.request.getFields.get(method.request.getFields.size-1)) <span>,</span>}</div>}}})
              {method.response.name}({if(method.response.fields.get(0).dataType.kind != KIND.VOID) <div>_result</div>} )
            </block>
          </block>
          </div>
        }
        }
        }


        class getServiceMetadata extends SoaFunctionDefinition.Sync[{service.namespace}.{service.name}, getServiceMetadata_args, getServiceMetadata_result](
        "getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer()) <block>


            @throws[TException]
            override def apply(iface: {service.namespace}.{service.name}, args: getServiceMetadata_args): getServiceMetadata_result = <block>

                    val source = scala.io.Source.fromInputStream({service.name}Codec.getClass.getClassLoader.getResourceAsStream("{oriNamespace}.{service.name}.xml"))
                    try getServiceMetadata_result(source.mkString) finally source.close

            </block>
            </block>

        class echo extends SoaFunctionDefinition.Sync[{service.namespace}.{service.name}, echo_args, echo_result](
        "echo", new echo_argsSerializer(), new echo_resultSerializer()) <block>


          @throws[TException]
          override def apply(iface: {service.namespace}.{service.name}, args: echo_args): echo_result = <block>

            echo_result(TransactionContext.Factory.currentInstance().getAttribute("container-threadPool-info")+"")
            //echo_result("PONG")

          </block>
        </block>

      class Processor(iface: {service.getNamespace}.{service.name}, ifaceClass: Class[{service.getNamespace}.{service.name}] ) extends
        SoaServiceDefinition(iface,classOf[{service.getNamespace}.{service.name}], Processor.getProcessMap)

        object Processor<block>

          type PF = SoaFunctionDefinition[{service.getNamespace}.{service.name}, _, _]

          def getProcessMap(): java.util.Map[String, PF] = <block>
             val map = new java.util.HashMap[String, PF]()
            {toMethodArrayBuffer(service.getMethods).map{(method: Method)=>{
              <div>map.put("{method.name}", new {method.name})
              </div>}}}
            map.put("getServiceMetadata", new getServiceMetadata)
            map.put("echo", new echo)
            map
          </block>

        </block>
        </block>
      </div>
    }
  }

  def toAsyncCodecTemplate(service:Service,structNamespaces:util.Set[String], oriNamespace: String): Elem = {
    //val structNameCache = new util.ArrayList[String]()

    return {
      <div>package {service.namespace.substring(0, service.namespace.lastIndexOf("."))}
        {
        import collection.JavaConversions._
        structNamespaces.map(struct => {
          <div>import {struct}.serializer._;</div>
        })
        }

        import com.github.dapeng.core._
        import com.github.dapeng.org.apache.thrift._
        import com.github.dapeng.org.apache.thrift.protocol._
        import com.github.dapeng.core.definition._
        import {service.namespace.substring(0, service.namespace.lastIndexOf("."))}.{service.name}SuperCodec._

        import scala.concurrent.ExecutionContext.Implicits.global
        import java.util.concurrent.<block>CompletableFuture, Future</block>
        import scala.util.<block>Failure, Success</block>

        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *
        **/
        object {service.name}AsyncCodec <block>

        implicit class FutureX[T](f: scala.concurrent.Future[T]) <block>
          def tojava(): CompletableFuture[T] = <block>
            val java = new CompletableFuture[T]()
            f.onComplete<block>
              case Success(x) => java.complete(x)
              case Failure(ex) => java.completeExceptionally(ex)
            </block>
            java
          </block>
        </block>

        {toMethodArrayBuffer(service.methods).map{(method: Method)=> {

          <div>
            class {method.name} extends SoaFunctionDefinition.Async[{service.namespace}.{service.name}Async, {method.name}_args, {method.name}_result]("{method.name}", new {method.name.charAt(0).toUpper + method.name.substring(1)}_argsSerializer(), new {method.name.charAt(0).toUpper + method.name.substring(1)}_resultSerializer())<block>

            @throws[TException]
            def apply(iface: {service.namespace}.{service.name}Async, args: {method.name}_args):Future[{method.name}_result] = <block>

              val _result = iface.{method.name}({toFieldArrayBuffer(method.request.getFields).map{(field: Field)=>{<div>args.{nameAsId(field.name)}{if(field != method.request.getFields.get(method.request.getFields.size-1)) <span>,</span>}</div>}}})

              {
              if(method.getResponse.getFields.get(0).getDataType.kind != DataType.KIND.VOID)
                <div>_result.map({method.name}_result(_)).tojava</div>
              else <div>
                _result.map(i => {method.name}_result()).tojava

              </div>
              }

            </block>
          </block>
          </div>
        }
        }
        }

        class getServiceMetadata extends SoaFunctionDefinition.Async[{service.namespace}.{service.name}Async, getServiceMetadata_args, getServiceMetadata_result](
        "getServiceMetadata", new GetServiceMetadata_argsSerializer(), new GetServiceMetadata_resultSerializer()) <block>


          @throws[TException]
          override def apply(iface: {service.namespace}.{service.name}Async, args: getServiceMetadata_args): Future[getServiceMetadata_result] = <block>

            val result = scala.concurrent.Future <block>
              val source = scala.io.Source.fromInputStream({service.name}Codec.getClass.getClassLoader.getResourceAsStream("{oriNamespace}.{service.name}.xml"))
              try getServiceMetadata_result(source.mkString) finally source.close
            </block>
            result.tojava

          </block>
        </block>

        class echo extends SoaFunctionDefinition.Async[{service.namespace}.{service.name}Async, echo_args, echo_result](
        "echo", new echo_argsSerializer(), new echo_resultSerializer()) <block>


          @throws[TException]
          override def apply(iface: {service.namespace}.{service.name}Async, args: echo_args): Future[echo_result] = <block>

            val result = scala.concurrent.Future <block>

              //echo_result("PONG")
              echo_result(TransactionContext.Factory.currentInstance().getAttribute("container-threadPool-info")+"")

            </block>
            result.tojava

          </block>
        </block>

        class Processor(iface: {service.getNamespace}.{service.name}Async, ifaceClass: Class[{service.getNamespace}.{service.name}Async]) extends
        SoaServiceDefinition(iface,classOf[{service.getNamespace}.{service.name}Async], Processor.buildMap)

        object Processor<block>

          type PF = SoaFunctionDefinition[{service.getNamespace}.{service.name}Async, _, _]

          def buildMap(): java.util.Map[String, PF] = <block>
            val map = new java.util.HashMap[String, PF]()
            {toMethodArrayBuffer(service.getMethods).map{(method: Method)=>{
              <div>map.put("{method.name}", new {method.name})
              </div>}}}
            map.put("getServiceMetadata", new getServiceMetadata)
            map
          </block>

        </block>
      </block>
      </div>
    }
  }


  def toStructSerializerTemplate(struct:Struct ,structNamespaces:util.Set[String]): Elem ={
    return {
      <div> package {struct.namespace}.serializer;

        {
        import collection.JavaConversions._
        structNamespaces.map(struct => {
          <div>import {struct}.serializer._;</div>
        })
        }
        import com.github.dapeng.core._
        import com.github.dapeng.org.apache.thrift._
        import com.github.dapeng.org.apache.thrift.protocol._

        /**
        * Autogenerated by Dapeng-Code-Generator (2.1.1)
        *
        * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
        *
        **/

        <div>class {struct.name}Serializer extends BeanSerializer[{struct.getNamespace() + "." + struct.name}]<block>
          {getReadMethod(struct)}{getWriteMethod(struct)}{getValidateMethod(struct)}

          @throws[TException]
          override def toString(bean: {struct.namespace}.{struct.name}): String = if (bean == null) "null" else bean.toString

        </block>
        </div>
      </div>
    }
  }

  def toStructName(struct: Struct): String = {
    if (struct.getNamespace == null) {
      return struct.getName()
    } else {
      return struct.getNamespace + "." + struct.getName()
    }
  }

  def toThriftDateType(dataType:DataType): Elem = {
    dataType.kind match {
      case KIND.VOID => <div>com.github.dapeng.org.apache.thrift.protocol.TType.VOID</div>
      case KIND.BOOLEAN => <div>com.github.dapeng.org.apache.thrift.protocol.TType.BOOL</div>
      case KIND.BYTE => <div>com.github.dapeng.org.apache.thrift.protocol.TType.BYTE</div>
      case KIND.SHORT => <div>com.github.dapeng.org.apache.thrift.protocol.TType.I16</div>
      case KIND.INTEGER => <div>com.github.dapeng.org.apache.thrift.protocol.TType.I32</div>
      case KIND.LONG => <div>com.github.dapeng.org.apache.thrift.protocol.TType.I64</div>
      case KIND.DOUBLE => <div>com.github.dapeng.org.apache.thrift.protocol.TType.DOUBLE</div>
      case KIND.STRING => <div>com.github.dapeng.org.apache.thrift.protocol.TType.STRING</div>
      case KIND.MAP => <div>com.github.dapeng.org.apache.thrift.protocol.TType.MAP</div>
      case KIND.LIST => <div>com.github.dapeng.org.apache.thrift.protocol.TType.LIST</div>
      case KIND.SET => <div>com.github.dapeng.org.apache.thrift.protocol.TType.SET</div>
      case KIND.ENUM => <div>com.github.dapeng.org.apache.thrift.protocol.TType.I32</div>
      case KIND.STRUCT => <div>com.github.dapeng.org.apache.thrift.protocol.TType.STRUCT</div>
      case KIND.DATE => <div>com.github.dapeng.org.apache.thrift.protocol.TType.I64</div>
      case KIND.BIGDECIMAL => <div>com.github.dapeng.org.apache.thrift.protocol.TType.STRING</div>
      case KIND.BINARY => <div>com.github.dapeng.org.apache.thrift.protocol.TType.STRING</div>
      case _ => <div></div>
    }
  }


  def toScalaDataType(dataType:DataType): Elem = {
    dataType.kind match {
      case KIND.VOID => <div></div>
      case KIND.BOOLEAN => <div>Boolean</div>
      case KIND.BYTE => <div>Byte</div>
      case KIND.SHORT => <div>Short</div>
      case KIND.INTEGER => <div>Int</div>
      case KIND.LONG => <div>Long</div>
      case KIND.DOUBLE => <div>Double</div>
      case KIND.STRING => <div>String</div>
      case KIND.BINARY => <div>java.nio.ByteBuffer</div>
      case KIND.DATE => <div>java.util.Date</div>
      case KIND.BIGDECIMAL => <div>BigDecimal</div>
      case KIND.MAP =>
        return {<div>Map[{toScalaDataType(dataType.getKeyType)}, {toScalaDataType(dataType.getValueType)}]</div>}
      case KIND.LIST =>
        return {<div>List[{toScalaDataType(dataType.getValueType)}]</div>}
      case KIND.SET =>
        return {<div>Set[{toScalaDataType(dataType.getValueType)}]</div>}
      case KIND.ENUM =>
        return {<div>{dataType.getQualifiedName}</div>}
      case KIND.STRUCT =>
        return {<div>{dataType.getQualifiedName}</div>}
    }
  }

  def getDefaultValueWithType(dataType: DataType): Elem = {

    dataType.kind match {
      case KIND.BOOLEAN => <div>false</div>
      case KIND.BYTE => <div>0</div>
      case KIND.SHORT => <div>0</div>
      case KIND.INTEGER => <div>0</div>
      case KIND.LONG => <div>0</div>
      case KIND.DOUBLE => <div>0.00</div>
      case KIND.STRING => <div>null</div>
      case KIND.BINARY => <div>null</div>
      case KIND.DATE => <div>null</div>
      case KIND.BIGDECIMAL => <div>null</div>
      case KIND.LIST => <div>List.empty</div>
      case KIND.SET => <div>Set.empty</div>
      case KIND.MAP => <div>Map.empty</div>
      case _ => <div>null</div>
    }

  }


  def getScalaReadElement(dataType: DataType, index: Int):Elem = {
    dataType.kind match {
      case KIND.BOOLEAN => <div>iprot.readBool</div>
      case KIND.STRING => <div>iprot.readString</div>
      case KIND.BYTE => <div>iprot.readByte</div>
      case KIND.SHORT =>  <div>iprot.readI16</div>
      case KIND.INTEGER => <div>iprot.readI32</div>
      case KIND.LONG => <div>iprot.readI64</div>
      case KIND.DOUBLE => <div>iprot.readDouble</div>
      case KIND.BINARY => <div>iprot.readBinary</div>
      case KIND.BIGDECIMAL => <div>BigDecimal(iprot.readString)</div>
      case KIND.DATE => <div>new java.util.Date(iprot.readI64)</div>
      case KIND.STRUCT =>
        <div>
          new {dataType.qualifiedName.substring(0,dataType.qualifiedName.lastIndexOf("."))+".serializer."+dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".")+1)}Serializer().read(iprot)
        </div>
      case KIND.ENUM =>
        <div>
          {dataType.qualifiedName}.findByValue(iprot.readI32)
        </div>
      case KIND.MAP => <div><block>
        val _map{index} : com.github.dapeng.org.apache.thrift.protocol.TMap = iprot.readMapBegin
        val _result{index} = (0 until _map{index}.size).map(_ => <block>( {getScalaReadElement(dataType.keyType, index+1)} -> {getScalaReadElement(dataType.valueType, index+2)} )</block>).toMap
        iprot.readMapEnd
        _result{index}
      </block></div>
      case KIND.LIST => <div><block>
        val _list{index} : com.github.dapeng.org.apache.thrift.protocol.TList = iprot.readListBegin
        val _result{index} = (0 until _list{index}.size).map(_ => <block>{getScalaReadElement(dataType.valueType, index+1)}</block>).toList
        iprot.readListEnd
        _result{index}
        </block></div>
      case KIND.SET => <div><block>
        val _set{index} : com.github.dapeng.org.apache.thrift.protocol.TSet = iprot.readSetBegin
        val _result{index} = (0 until _set{index}.size).map(_ => <block>{getScalaReadElement(dataType.valueType, index+1)}</block>).toSet
        iprot.readSetEnd
        _result{index}
      </block></div>
      case KIND.VOID => <div>com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)</div>
      case _ => <div></div>
    }
  }

  def getReadMethod(struct: Struct): Elem = {
    <div>
      @throws[TException]
      override def read(iprot: TProtocol): {toStructName(struct)} = <block>

        var schemeField: com.github.dapeng.org.apache.thrift.protocol.TField = null
        iprot.readStructBegin()

      {toFieldArrayBuffer(struct.getFields).map{(field : Field) =>{
        <div>var {nameAsId(field.name)}: {if(field.isOptional) <div>Option[</div>}{toScalaDataType(field.dataType)}{if(field.isOptional) <div>]</div>} = {if(field.isOptional) <div>None</div> else getDefaultValueWithType(field.dataType)}
        </div>
      }}}

      while (schemeField == null || schemeField.`type` != com.github.dapeng.org.apache.thrift.protocol.TType.STOP) <block>

        schemeField = iprot.readFieldBegin

        schemeField.id match <block>
          {toFieldArrayBuffer(struct.getFields).map{(field : Field) =>{
            <div>
              case {field.tag} =>
                  schemeField.`type` match <block>
                    case {toThriftDateType(field.dataType)} => {nameAsId(field.name)} = {if(field.isOptional) <div>Option(</div>}{getScalaReadElement(field.dataType, 0)}{if(field.isOptional) <div>)</div>}
                    case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
            </block>
            </div>
          }}}
          case _ => com.github.dapeng.org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.`type`)
        </block>
      </block>

      iprot.readFieldEnd
      iprot.readStructEnd

      val bean = {toStructName(struct)}({toFieldArrayBuffer(struct.getFields).map{(field : Field) =>{<div>{nameAsId(field.name)} = {nameAsId(field.name)}{if(field != struct.getFields.get(struct.getFields.size-1)) <span>,</span>}</div>}}})
      validate(bean)

      bean
      </block>
    </div>
  }


  def toScalaWriteElement(dataType: DataType, index: Int):Elem = {

    dataType.kind match {
      case KIND.BOOLEAN => <div>oprot.writeBool(elem{index})</div>
      case KIND.BYTE => <div>oprot.writeByte(elem{index})</div>
      case KIND.SHORT => <div>oprot.writeI16(elem{index})</div>
      case KIND.INTEGER => <div>oprot.writeI32(elem{index})</div>
      case KIND.LONG => <div>oprot.writeI64(elem{index})</div>
      case KIND.DOUBLE => <div>oprot.writeDouble(elem{index})</div>
      case KIND.STRING => <div>oprot.writeString(elem{index})</div>
      case KIND.ENUM => <div>oprot.writeI32(elem{index}.id)</div>
      case KIND.BINARY => <div>oprot.writeBinary(elem{index})</div>
      case KIND.DATE => <div>oprot.writeI64(elem{index}.getTime)</div>
      case KIND.BIGDECIMAL => <div>oprot.writeString(elem{index}.toString)</div>
      case KIND.STRUCT =>
        <div>
          new {dataType.qualifiedName.substring(0,dataType.qualifiedName.lastIndexOf("."))+".serializer."+dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".")+1)}Serializer().write(elem{index}, oprot)
        </div>
      case KIND.LIST => <div>
        oprot.writeListBegin(new com.github.dapeng.org.apache.thrift.protocol.TList({toThriftDateType(dataType.valueType)}, elem{index}.size))
        elem{index}.foreach(elem{index+1} => <block>{toScalaWriteElement(dataType.valueType, index+1)}</block>)
        oprot.writeListEnd
      </div>
      case KIND.MAP => <div>
        oprot.writeMapBegin(new com.github.dapeng.org.apache.thrift.protocol.TMap({toThriftDateType(dataType.keyType)}, {toThriftDateType(dataType.valueType)}, elem{index}.size))

        elem{index}.map<block>case(elem{index+1}, elem{index+2}) => <block>
          {toScalaWriteElement(dataType.keyType, index+1)}
          {toScalaWriteElement(dataType.valueType, index+2)}
        </block>
        </block>
        oprot.writeMapEnd
      </div>
      case KIND.SET => <div>
        oprot.writeSetBegin(new com.github.dapeng.org.apache.thrift.protocol.TSet({toThriftDateType(dataType.valueType)}, elem{index}.size))
        elem{index}.foreach(elem{index+1} => <block>{toScalaWriteElement(dataType.valueType, index+1)}</block>)
        oprot.writeSetEnd
      </div>
      case _ => <div></div>
    }
  }


  def getWriteMethod(struct: Struct): Elem = {

    var index = 0
    <div>
      @throws[TException]
      override def write(bean: {toStructName(struct)}, oprot: TProtocol): Unit = <block>

      validate(bean)
      oprot.writeStructBegin(new com.github.dapeng.org.apache.thrift.protocol.TStruct("{struct.name}"))

      {toFieldArrayBuffer(struct.fields).map{(field : Field) =>{
        if(field.dataType.getKind() == DataType.KIND.VOID) {}
        else {
          <div>
            {if(field.isOptional) <div>if(bean.{nameAsId(field.name)}.isDefined)</div>}<block>{ <div>if(!bean.{nameAsId(field.name)}.equals(Some(null)))</div>}<block>
            val elem{index} = bean.{nameAsId(field.name)} {if(field.isOptional) <div>.get</div>}
            oprot.writeFieldBegin(new com.github.dapeng.org.apache.thrift.protocol.TField("{nameAsId(field.name)}", {toThriftDateType(field.dataType)}, {field.tag}.asInstanceOf[Short]))
            {toScalaWriteElement(field.dataType, index)}
            oprot.writeFieldEnd
            {index = index + 1}
            </block></block></div>
        }
      }
      }
      }
      oprot.writeFieldStop
      oprot.writeStructEnd
    </block>
    </div>
  }

  def getValidateMethod(struct: Struct) : Elem = {
    <div>
      @throws[TException]
      override def validate(bean: {toStructName(struct)}): Unit = <block>
      {
      toFieldArrayBuffer(struct.fields).map{(field : Field) =>{
        <div>{
          if(!field.isOptional && field.dataType.kind != DataType.KIND.VOID && checkIfNeedValidate(field.isOptional, field.dataType)){
            <div>
              if(bean.{nameAsId(field.name)} == null)
              throw new SoaException(SoaCode.StructFieldNull, "{nameAsId(field.name)}字段不允许为空")
            </div>}}</div>
          <div>{
            if(!field.isOptional && field.dataType.kind == KIND.STRUCT && field.dataType.kind != DataType.KIND.VOID){
              <div>
                if(bean.{nameAsId(field.name)} != null)
                new {field.dataType.qualifiedName.substring(0,field.dataType.qualifiedName.lastIndexOf("."))+".serializer."+field.dataType.qualifiedName.substring(field.dataType.qualifiedName.lastIndexOf(".")+1)}Serializer().validate(bean.{nameAsId(field.name)})
              </div>}}</div>
          <div>{
            if(field.isOptional && field.dataType.kind == KIND.STRUCT && field.dataType.kind != DataType.KIND.VOID){
              <div>
                if(bean.{nameAsId(field.name)}.isDefined )
                new {if(field.dataType.qualifiedName.contains("com.github.dapeng.soa.scala")) field.dataType.qualifiedName.substring(0,field.dataType.qualifiedName.lastIndexOf("."))+".serializer."+field.dataType.qualifiedName.substring(field.dataType.qualifiedName.lastIndexOf(".")+1) else field.dataType.qualifiedName.substring(0,field.dataType.qualifiedName.lastIndexOf(".")).replace("com.github.dapeng.soa","com.github.dapeng.soa.scala")+".serializer."+field.dataType.qualifiedName.substring(field.dataType.qualifiedName.lastIndexOf(".")+1)}Serializer().validate(bean.{nameAsId(field.name)}.get)
              </div>}}</div>
      }
      }
      }
    </block>
    </div>
  }

  def checkIfNeedValidate(optional: Boolean, dataType: DataType): Boolean = {

    if(optional)
      false
    else
      dataType.kind match {

        case DataType.KIND.BOOLEAN => false
        case KIND.SHORT => false
        case KIND.INTEGER => false
        case KIND.LONG => false
        case KIND.DOUBLE => false
        case _ => true
      }

  }


  def caseClassFiledCombile(field: Field): Elem ={
    <div>{if(field.dataType.kind != KIND.VOID) <div>{nameAsId(field.name)}:{toScalaDataType(field.dataType)}</div>}</div>

  }




//  def getToStringElement(field: Field): Elem = {
//    <div>
//      stringBuilder.append("\"").append("{nameAsId(field.name)}").append("\":{if(field.dataType.kind == DataType.KIND.STRING) <div>\"</div>}").append({getToStringByDataType(field)}).append("{if(field.dataType.kind == DataType.KIND.STRING) <div>\"</div>},");
//    </div>
//  }
//
//  def getToStringByDataType(field: Field):Elem = {
//    if(field.dataType.kind == KIND.STRUCT) <div>this.{nameAsId(field.name)} == null ? "null" : this.{nameAsId(field.name)}.toString()</div> else <div>{nameAsId(field.name)}</div>
//  }

}
