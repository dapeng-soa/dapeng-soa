/// <reference path="../ts-lib/jquery.d.ts"/>
/// <reference path="../ts-lib/jquerytemplate.d.ts"/>
/// <reference path="model.ts"/>
var api;
(function (api) {
    var StructAction = /** @class */ (function () {
        function StructAction() {
        }
        StructAction.prototype.findStruct = function (serviceName, version, fullStructName, isModel) {
            if (isModel === void 0) { isModel = false; }
            this.serviceName = serviceName;
            this.version = version;
            this.fullStructName = fullStructName;
            this.isModel = isModel;
            var url = window.basePath + "/api/findstruct/" + serviceName + "/" + version + "/" + fullStructName + ".htm";
            var settings = { type: "get", url: url, dataType: "json" };
            var self = this;
            $.ajax(settings)
                .done(function (result) {
                if (isModel) {
                    self.resultToHTML(result);
                    window.$previousModle.push($("#struct-model .struct-model-context").html());
                    if (window.$previousModle.length > 1) {
                        $(".back-to-previous").show();
                    }
                }
                else {
                    for (var index = 0; index < result.fields.length; index++) {
                        $(".struct-field-datatype-" + index).html(self.dataTypeToHTML(result.fields[index].dataType));
                    }
                }
            });
        };
        StructAction.prototype.resultToHTML = function (result) {
            var self = this;
            var structurl = window.basePath + "/api/struct/" + this.serviceName + "/" + this.version + "/" + this.fullStructName + ".htm";
            $("#struct-model .struct-model-context").html("\n                <div class=\"row\">\n                <div class=\"col-sm-12 col-md-12\">\n                <div class=\"page-header mt5\">\n                <h1>" + result.name + "</h1>\n                </div>\n                <p data-marked-id=\"marked\">" + (result.doc == null ? '没有相关描述' : result.doc) + "</p>\n\n                <h3>\u5750\u6807</h3>\n                <table class=\"table table-bordered\">\n                <thead>\n                <tr class=\"breadcrumb\">\n                    <th>\u670D\u52A1\u540D</th>\n                    <th>\u7248\u672C\u53F7</th>\n                    <th>\u7ED3\u6784\u4F53\u5168\u9650\u5B9A\u540D</th>\n                </tr>\n                </thead>\n                <tbody>\n                <tr>\n                    <td>" + result.name + "</td>\n                    <td>" + self.version + "</td>\n                    <td>" + result.namespace + "." + result.name + "</td>\n                </tr>\n                </tbody>\n            </table>\n\n            <h3>\u6570\u636E\u6210\u5458</h3>\n            <table class=\"table table-bordered\">\n                <thead>\n                <tr class=\"breadcrumb\">\n                    <th>#</th>\n                    <th>\u540D\u79F0</th>\n                    <th>\u7C7B\u578B</th>\n                    <th>\u662F\u5426\u5FC5\u586B</th>\n                    <th>\u63CF\u8FF0</th>\n                </tr>\n                </thead>\n                <tbody>\n                " + self.fieldsToHTML(result.fields) + "\n                </tbody>\n            </table>\n\n        </div>\n    </div>\n    </div>\n            ");
        };
        StructAction.prototype.fieldsToHTML = function (fields) {
            var fieldsStr = "";
            var self = this;
            for (var index = 0; index < fields.length; index++) {
                fieldsStr += "\n                                <tr>\n                        <td>" + fields[index].tag + "</td>\n                        <td>" + fields[index].name + "</td>\n                        <td class=\"struct-field-datatype-" + index + "\">" + self.dataTypeToHTML(fields[index].dataType) + "</td>\n                        <td>" + (fields[index].optional == true ? '<span class="label label-success">否</code>' : '<span class="label label-danger">是</span>') + "</td>\n                        <td data-marked-id=\"marked\">" + fields[index].doc + "</td>\n                        </tr>\n                            ";
            }
            return fieldsStr;
        };
        StructAction.prototype.dataTypeToHTML = function (dataType) {
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
                    return "\n                    <a href=\"javascript:void(0)\"\n                       onclick=getEnumDetail1('" + this.serviceName + "','" + this.version + "','" + dataType.qualifiedName + "')>\n                       " + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "\n                   </a>\n                    ";
                case api.model.KIND.STRUCT:
                    return "\n                            <a href=\"javascript:void(0)\"\n                               onclick=getStructDetail1('" + this.serviceName + "','" + this.version + "','" + dataType.qualifiedName + "')>\n                               " + dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1) + "\n                           </a>\n                        ";
                case api.model.KIND.DATE:
                    return "Date";
                case api.model.KIND.BIGDECIMAL:
                    return "BigDecimal";
                default:
                    return "Unknown";
            }
        };
        return StructAction;
    }());
    api.StructAction = StructAction;
})(api || (api = {}));
