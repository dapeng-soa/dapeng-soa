module api.model {

    export class KIND {
        public static VOID = "VOID"
        public static BOOLEAN = "BOOLEAN"
        public static BYTE = "BYTE"
        public static SHORT = "SHORT"
        public static INTEGER = "INTEGER"
        public static LONG = "LONG"
        public static DOUBLE = "DOUBLE"
        public static STRING = "STRING"
        public static BINARY = "BINARY"
        public static MAP = "MAP"
        public static LIST = "LIST"
        public static SET = "SET"
        public static ENUM = "ENUM"
        public static STRUCT = "STRUCT"
        public static DATE = "DATE"
        public static BIGDECIMAL = "BIGDECIMAL"
    }

    export class DataType {
        kind:string
        keyType:DataType
        valueType:DataType
        qualifiedName:string
    }

    export class Field {
        tag:number
        name:string
        optional:boolean
        dataType:DataType
        doc:string;
        privacy:boolean
    }

    export class Struct {
        namespace:string
        name:string
        label:string
        doc:string
        fields:Array<Field>
    }

    export class Method {
        name:string
        doc:string
        label:string
        request:Struct
        response:Struct
    }

}