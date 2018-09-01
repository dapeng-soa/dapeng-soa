com.github.dapeng.json 
==

一个Stream based API，来快速的处理 JSON

支持：
- JSON Stream -> Thrift Stream
- Thrift Stream -> JSON Stream

这个API 的设计的目的：

1、dapeng 支持高效的 基于Stream的JSON 处理，性能相比传统的API会有极速提升。
2、之前的 JSON 处理代码写的不是很好。


如下是一个实例的 JSONCallback 序列
- onStartObject
    - onStartFiled name 
        - onNumber
        - onBoolean
        - onString
        - onNull
        - onStartObject .. onEndObject
        - onStartArray
            - onNumber
            - onBoolean
            - onString
            - onNull
            - onStartObject .. onEndObject
            - onStartArray .. onEndArray
        - onEndArray
    - onEndField
- onEndObject

这里描述每一个事件的处理模式：
- onStartObject
    - 当前栈顶 ：STRUCT or MAP
    - 栈操作：无
    - 处理：proto.writeStructBegin or proto.writeMapBegin
- onEndObject
    - 当前栈顶：STRUCT or MAP
    - 栈操作：无
    - 处理：
        - proto.writeStructEnd or proto.writeMapEnd
        - 如果父栈元素是 LIST/SET/MAP，计数++
- onStartArray
    - 当前栈顶：LIST/SET
    - 栈操作：将当前栈顶的子元素（valueType）类型压栈
    - 处理：proto.writeListBegin or proto.writeSetBegin
- onEndArray
    - 当前栈顶：
    - 栈操作：pop 恢复当前栈顶为 LIST/SET
    - 处理：
        - proto.writeListEnd or proto.writeSetEnd
        - 如果父栈元素是 LIST/SET/MAP，计数++
- onStartField name
    - 当前栈顶：STRUCT / MAP
    - 栈操作：
        - STRUCT：将结构体的fields[name] 压栈
        - MAP：将 valueType 压栈
        - 同步新栈顶的 writeIndex
    - 处理：
        - STRUCT: proto.writeFieldBegin name
        - MAP: proto.writeString name or proto.writeInt name.toInt
- onEndField
    - 当前栈顶：any
    - 栈操作：pop 恢复当前 STRUCT/MAP
    - 处理：proto.writeFieldEnd
- onNumber
    - 当前栈顶：BYTE/SHORT/INTEGER/LONG/DOUBLE
    - 栈操作：无
    - 处理
        - proto.writeI8, writeI16, ...
        - 如果父栈元素是 LIST/SET/MAP，计数++
- onBoolean
    - 当前栈顶：BOOLEAN
    - 栈操作：无
    - 处理
        - proto.writeBoolean.
        - 如果父栈元素是 LIST/SET/MAP，计数++
- onString
    - 当前栈顶：STRING
    - 栈操作：无
    - 处理
        - proto.writeString, ...
        - 如果父栈元素是 LIST/SET/MAP，计数++
- onNull
    - 当前栈顶：any
    - 栈操作：无
    - 处理
        - 父栈元素为 STRUCT/MAP 时，当前field容许为空时，重置 writeIndex,否则报错
        - 父栈原色为 LIST/SET 时，不容许写入空值。

