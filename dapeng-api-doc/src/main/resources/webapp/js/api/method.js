/// <reference path="../ts-lib/jquery.d.ts"/>
/// <reference path="../ts-lib/jquerytemplate.d.ts"/>
/// <reference path="model.ts"/>
var api;
(function (api) {
    var MethodAction = /** @class */ (function () {
        function MethodAction() {
        }
        MethodAction.prototype.findMethod = function (serviceName, version, methodName, isModel) {
            if (isModel === void 0) { isModel = false; }
            this.serviceName = serviceName;
            this.version = version;
            this.methodName = methodName;
            this.isModel = isModel;
            var url = window.basePath + "/api/findmethod/" + serviceName + "/" + version + "/" + methodName + ".htm";
            var settings = { type: "get", url: url, dataType: "json" };
            var self = this;
            $.ajax(settings)
                .done(function (result) {
                for (var index = 0; index < result.request.fields.length; index++) {
                    $(".req-field-datatype-" + index).html(self.dataTypeToHTML(result.request.fields[index].dataType));
                }
                for (var index = 0; index < result.response.fields.length; index++) {
                    $(".resp-field-datatype-" + index).html(self.dataTypeToHTML(result.response.fields[index].dataType));
                }
            });
        };
        MethodAction.prototype.dataTypeToHTML = function (dataType) {
            switch (dataType.kind) {
                case api.model.KIND.VOID:
                    return "Void";
                case api.model.KIND.BOOLEAN:
                    return "Boolean";
                case api.model.KIND.BYTE:
                    return "Byte";
                case api.model.KIND.SHORT:
                    return "Short";
                case api.model.KIND.INTEGER:
                    return "Integer";
                case api.model.KIND.LONG:
                    return "Long";
                case api.model.KIND.DOUBLE:
                    return "Double";
                case api.model.KIND.STRING:
                    return "String";
                case api.model.KIND.BINARY:
                    return "byte[]";
                case api.model.KIND.MAP:
                    return "Map&lt;" + this.dataTypeToHTML(dataType.keyType) + ", " + this.dataTypeToHTML(dataType.valueType) + "&gt;";
                case api.model.KIND.LIST:
                    return "List&lt;" + this.dataTypeToHTML(dataType.valueType) + "&gt;";
                case api.model.KIND.SET:
                    return "Set&lt;" + this.dataTypeToHTML(dataType.valueType) + "&gt;";
                case api.model.KIND.ENUM:
                    if (this.isModel) {
                        return "\n                    <a href=\"javascript:void(0)\"\n                       onclick=getEnumDetail1('" + this.serviceName + "','" + this.version + "','" + dataType.qualifiedName + "')>\n                       " + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "\n                   </a>\n                    ";
                    }
                    else {
                        var enumurl = window.basePath + "/api/enum/" + this.serviceName + "/" + this.version + "/" + dataType.qualifiedName + ".htm";
                        return "<a href='" + enumurl + "'>" + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "</a>";
                    }
                case api.model.KIND.STRUCT:
                    if (this.isModel) {
                        return "\n                            <a href=\"javascript:void(0)\"\n                               onclick=getStructDetail1('" + this.serviceName + "','" + this.version + "','" + dataType.qualifiedName + "')>\n                               " + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "\n                           </a>\n                        ";
                    }
                    else {
                        var structurl = window.basePath + "/api/struct/" + this.serviceName + "/" + this.version + "/" + dataType.qualifiedName + ".htm";
                        return "<a href='" + structurl + "'>" + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "</a>";
                    }
                case api.model.KIND.DATE:
                    return "Date";
                case api.model.KIND.BIGDECIMAL:
                    return "BigDecimal";
                default:
                    return "Unknown";
            }
        };
        return MethodAction;
    }());
    api.MethodAction = MethodAction;
})(api || (api = {}));
