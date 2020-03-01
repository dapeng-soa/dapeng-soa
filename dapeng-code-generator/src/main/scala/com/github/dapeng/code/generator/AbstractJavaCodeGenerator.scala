package com.github.dapeng.code.generator

import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.{DataType, Field}

import scala.xml.Elem

abstract class AbstractJavaCodeGenerator extends CodeGenerator {

  protected def toFieldDeclareTemplate(field: Field) = {
    <div>{if(field.isOptional) <div>Optional{lt}</div>}{toDataTypeTemplate(field.isOptional, field.getDataType)}{if(field.isOptional) <div>{gt}</div>}</div>
  }

  protected def toDataTypeTemplate(optional: Boolean , dataType:DataType): Elem = {

    if (optional)
      toDataTypeTemplate(dataType)
    else
      dataType.kind match {
        case KIND.BOOLEAN => <div>boolean</div>
        case KIND.SHORT => <div>short</div>
        case KIND.INTEGER => <div>int</div>
        case KIND.LONG => <div>long</div>
        case KIND.DOUBLE => <div>double</div>
        case KIND.BYTE => <div>byte</div>
        case _ => toDataTypeTemplate(dataType)
      }
  }

  protected def toDataTypeTemplate(dataType:DataType): Elem = {
    dataType.kind match {
      case KIND.VOID => <div>void</div>
      case KIND.BOOLEAN => <div>Boolean</div>
      case KIND.BYTE => <div>Byte</div>
      case KIND.SHORT => <div>Short</div>
      case KIND.INTEGER => <div>Integer</div>
      case KIND.LONG => <div>Long</div>
      case KIND.DOUBLE => <div>Double</div>
      case KIND.STRING => <div>String</div>
      case KIND.BINARY => <div>java.nio.ByteBuffer</div>
      case KIND.DATE => <div>java.util.Date</div>
      case KIND.BIGDECIMAL => <div>java.math.BigDecimal</div>
      case KIND.MAP =>
      {<div>java.util.Map{lt}{toDataTypeTemplate(dataType.getKeyType())}, {toDataTypeTemplate(dataType.getValueType())}{gt}</div>}
      case KIND.LIST =>
      {<div>java.util.List{lt}{toDataTypeTemplate(dataType.getValueType())}{gt}</div>}
      case KIND.SET =>
      {<div>java.util.Set{lt}{toDataTypeTemplate(dataType.getValueType())}{gt}</div>}
      case KIND.ENUM =>
        val ref = dataType.getQualifiedName();
      {<div>{ref}</div>}
      case KIND.STRUCT =>
        val ref = dataType.getQualifiedName();
      {<div>{ref}</div>}
    }
  }
}
