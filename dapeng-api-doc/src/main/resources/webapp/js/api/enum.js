/// <reference path="../ts-lib/jquery.d.ts"/>
/// <reference path="../ts-lib/jquerytemplate.d.ts"/>
/// <reference path="model.ts"/>
var api;
(function (api) {
    var EnumAction = /** @class */ (function () {
        function EnumAction() {
        }
        EnumAction.prototype.findEnum = function (serviceName, version, fullStructName, isModel) {
            if (isModel === void 0) { isModel = false; }
            this.serviceName = serviceName;
            this.version = version;
            this.fullStructName = fullStructName;
            this.isModel = isModel;
            var url = window.basePath + "/api/findEnum/" + serviceName + "/" + version + "/" + fullStructName + ".htm";
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
            });
        };
        EnumAction.prototype.resultToHTML = function (result) {
            var enumurl = window.basePath + "/api/enum/" + this.serviceName + "/" + this.version + "/" + this.fullStructName + ".htm";
            var self = this;
            $("#struct-model .struct-model-context").html("\n             <div class=\"row\">\n        <div class=\"col-sm-12 col-md-12\">\n            <div class=\"page-header mt5\">\n                <h1>" + result.name + "</h1>\n            </div>\n            <p data-marked-id=\"marked\">" + result.doc + "</p>\n\n            <h3>\u5750\u6807</h3>\n            <table class=\"table table-bordered\">\n                <thead>\n                <tr class=\"breadcrumb\">\n                    <th>\u670D\u52A1\u540D</th>\n                    <th>\u7248\u672C\u53F7</th>\n                    <th>\u7ED3\u6784\u4F53\u5168\u9650\u5B9A\u540D</th>\n                </tr>\n                </thead>\n                <tbody>\n                <tr>\n                    <td>" + result.name + "</td>\n                    <td>" + self.version + "</td>\n                    <td>" + result.namespace + "." + result.name + "</td>\n                </tr>\n                </tbody>\n            </table>\n\n            <h3>\u679A\u4E3E\u6210\u5458</h3>\n            <table class=\"table table-bordered\">\n                <thead>\n                <tr class=\"breadcrumb\">\n                    <th>#</th>\n                    <th>\u540D\u79F0</th>\n                    <th>\u503C</th>\n                    <th>\u63CF\u8FF0</th>\n                </tr>\n                </thead>\n                <tbody>\n                " + self.enumItemsToHTML(result.enumItems) + "\n                </tbody>\n            </table>\n\n        </div>\n    </div>\n            ");
        };
        EnumAction.prototype.enumItemsToHTML = function (enumItems) {
            var enumItemsStr = "";
            for (var index = 0; index < enumItems.length; index++) {
                enumItemsStr += "\n                               <tr>\n                        <td>" + (index + 1) + "</td>\n                        <td>" + enumItems[index].label + "</td>\n                        <td>" + enumItems[index].value + "</td>\n                        <td data-marked-id=\"marked\">" + enumItems[index].doc + "</td>\n                    </tr>\n                            ";
            }
            return enumItemsStr;
        };
        return EnumAction;
    }());
    api.EnumAction = EnumAction;
})(api || (api = {}));
