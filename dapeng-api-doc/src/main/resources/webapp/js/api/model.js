var api;
(function (api) {
    var model;
    (function (model) {
        var KIND = /** @class */ (function () {
            function KIND() {
            }
            KIND.VOID = "VOID";
            KIND.BOOLEAN = "BOOLEAN";
            KIND.BYTE = "BYTE";
            KIND.SHORT = "SHORT";
            KIND.INTEGER = "INTEGER";
            KIND.LONG = "LONG";
            KIND.DOUBLE = "DOUBLE";
            KIND.STRING = "STRING";
            KIND.BINARY = "BINARY";
            KIND.MAP = "MAP";
            KIND.LIST = "LIST";
            KIND.SET = "SET";
            KIND.ENUM = "ENUM";
            KIND.STRUCT = "STRUCT";
            KIND.DATE = "DATE";
            KIND.BIGDECIMAL = "BIGDECIMAL";
            return KIND;
        }());
        model.KIND = KIND;
        var DataType = /** @class */ (function () {
            function DataType() {
            }
            return DataType;
        }());
        model.DataType = DataType;
        var Field = /** @class */ (function () {
            function Field() {
            }
            return Field;
        }());
        model.Field = Field;
        var Struct = /** @class */ (function () {
            function Struct() {
            }
            return Struct;
        }());
        model.Struct = Struct;
        var Method = /** @class */ (function () {
            function Method() {
            }
            return Method;
        }());
        model.Method = Method;
    })(model = api.model || (api.model = {}));
})(api || (api = {}));
