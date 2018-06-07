/// <reference path="../ts-lib/jquery.d.ts"/>
/// <reference path="../ts-lib/jquerytemplate.d.ts"/>
/// <reference path="model.ts"/>

module api {

    export class StructAction {
        serviceName:string;
        version:string;
        fullStructName:string;
        isModel:Boolean;

        public findStruct(serviceName:string, version:string, fullStructName:string,isModel=false) {
            this.serviceName = serviceName;
            this.version = version;
            this.fullStructName = fullStructName;
            this.isModel = isModel;

            let url = window.basePath + "/api/findstruct/" + serviceName + "/" + version + "/" + fullStructName + ".htm"

            let settings:JQueryAjaxSettings = {type: "get", url: url, dataType: "json"}

            let self = this;

            $.ajax(settings)
                .done(function (result:api.model.Struct) {
                    if(isModel){
                        self.resultToHTML(result);
                        window.$previousModle.push($("#struct-model .struct-model-context").html());
                        if(window.$previousModle.length > 1){
                            $(".back-to-previous").show();
                        }
                    }else {
                        for (let index = 0; index < result.fields.length; index++) {
                            $(".struct-field-datatype-" + index).html(self.dataTypeToHTML(result.fields[index].dataType))
                        }
                    }
                });
        }

        private resultToHTML(result:api.model.Struct){
            const self = this;
            let structurl = window.basePath + "/api/struct/" + this.serviceName + "/" + this.version + "/" + this.fullStructName + ".htm";
            $("#struct-model .struct-model-context").html(`
                <div class="row">
                <div class="col-sm-12 col-md-12">
                <div class="page-header mt5">
                <h1>${result.name}</h1>
                </div>
                <p data-marked-id="marked">${result.doc==null?'没有相关描述':result.doc}</p>

                <h3>坐标</h3>
                <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>服务名</th>
                    <th>版本号</th>
                    <th>结构体全限定名</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>${result.name}</td>
                    <td>${self.version}</td>
                    <td>${result.namespace}.${result.name}</td>
                </tr>
                </tbody>
            </table>

            <h3>数据成员</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>#</th>
                    <th>名称</th>
                    <th>类型</th>
                    <th>是否必填</th>
                    <th>描述</th>
                </tr>
                </thead>
                <tbody>
                ${self.fieldsToHTML(result.fields)}
                </tbody>
            </table>

        </div>
    </div>
    </div>
            `)
        }

        private fieldsToHTML(fields:Array<api.model.Field>){
            let fieldsStr:String = "";
            const self = this;
            for (let index = 0; index < fields.length; index++) {
                fieldsStr += `
                                <tr>
                        <td>${fields[index].tag}</td>
                        <td>${fields[index].name}</td>
                        <td class="struct-field-datatype-${index}">${self.dataTypeToHTML(fields[index].dataType)}</td>
                        <td>${fields[index].optional == true ? '<span class="label label-success">否</code>' : '<span class="label label-danger">是</span>'}</td>
                        <td data-marked-id="marked">${fields[index].doc}</td>
                        </tr>
                            `
            }
            return fieldsStr;
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

                    return `
                    <a href="javascript:void(0)"
                       onclick=getEnumDetail1('${this.serviceName}','${this.version}','${dataType.qualifiedName}')>
                       ${dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1)}
                   </a>
                    `;

                case api.model.KIND.STRUCT:

                    return `
                            <a href="javascript:void(0)"
                               onclick=getStructDetail1('${this.serviceName}','${this.version}','${dataType.qualifiedName}')>
                               ${dataType.qualifiedName.substring(dataType.qualifiedName.lastIndexOf(".") + 1)}
                           </a>
                        `;
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