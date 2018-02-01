/// <reference path="../ts-lib/jquery.d.ts"/>
/// <reference path="../ts-lib/jquerytemplate.d.ts"/>
/// <reference path="model.ts"/>

module api {

    export class StructAction {
        serviceName:string
        version:string
        fullStructName:string

        public findStruct(serviceName:string, version:string, fullStructName:string) {
            this.serviceName = serviceName
            this.version = version
            this.fullStructName = fullStructName

            var url = window.basePath + "/api/findstruct/" + serviceName + "/" + version + "/" + fullStructName + ".htm"

            var settings:JQueryAjaxSettings = {type: "get", url: url, dataType: "json"}

            var self = this

            $.ajax(settings)
                .done(function (result:api.model.Struct) {
                    for (var index = 0; index < result.fields.length; index++) {
                        $(".struct-field-datatype-" + index).html(self.dataTypeToHTML(result.fields[index].dataType))
                    }
                });
        }

        private dataTypeToHTML(dataType:api.model.DataType):string {
            switch (dataType.kind) {
                case api.model.KIND.VOID:
                    return "Void"
                case api.model.KIND.BOOLEAN:
                    return "Boolean"
                case api.model.KIND.BYTE:
                    return "Byte"
                case api.model.KIND.SHORT:
                    return "Short"
                case api.model.KIND.INTEGER:
                    return "Integer"
                case api.model.KIND.LONG:
                    return "Long"
                case api.model.KIND.DOUBLE:
                    return "Double"
                case api.model.KIND.STRING:
                    return "String"
                case api.model.KIND.BINARY:
                    return "byte[]"
                case api.model.KIND.MAP:
                    return "Map&lt;" + this.dataTypeToHTML(dataType.keyType) + ", " + this.dataTypeToHTML(dataType.valueType) + "&gt;"
                case api.model.KIND.LIST:
                    return "List&lt;" + this.dataTypeToHTML(dataType.valueType) + "&gt;"
                case api.model.KIND.SET:
                    return "Set&lt;" + this.dataTypeToHTML(dataType.valueType) + "&gt;"
                case api.model.KIND.ENUM:
                    var enumurl = window.basePath + "/api/enum/" + this.serviceName + "/" + this.version + "/" + dataType.qualifiedName + ".htm"

                    return "<a href='" + enumurl + "'>" + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "</a>"
                case api.model.KIND.STRUCT:
                    var structurl = window.basePath + "/api/struct/" + this.serviceName + "/" + this.version + "/" + dataType.qualifiedName + ".htm"

                    return "<a href='" + structurl + "'>" + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "</a>"
                case api.model.KIND.DATE:
                    return "Date"
                case api.model.KIND.BIGDECIMAL:
                    return "BigDecimal"
                default:
                    return "Unknown"
            }
        }

    }

}