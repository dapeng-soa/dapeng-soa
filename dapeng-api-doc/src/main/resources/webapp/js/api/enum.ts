/// <reference path="../ts-lib/jquery.d.ts"/>
/// <reference path="../ts-lib/jquerytemplate.d.ts"/>
/// <reference path="model.ts"/>

module api {

    export class EnumAction {
        serviceName: string;
        version: string;
        fullStructName: string;
        isModel: Boolean;

        public findEnum(serviceName: string, version: string, fullStructName: string, isModel = false) {
            this.serviceName = serviceName;
            this.version = version;
            this.fullStructName = fullStructName;
            this.isModel = isModel;

            var url = window.basePath + "/api/findEnum/" + serviceName + "/" + version + "/" + fullStructName + ".htm"

            var settings: JQueryAjaxSettings = {type: "get", url: url, dataType: "json"}

            var self = this

            $.ajax(settings)
                .done(function (result) {
                    if(isModel){
                        self.resultToHTML(result);
                        window.$previousModle.push($("#struct-model .struct-model-context").html());
                        if(window.$previousModle.length > 1){
                            $(".back-to-previous").show();
                        }
                    }
                });
        }

        private resultToHTML(result) {
            let enumurl = window.basePath + "/api/enum/" + this.serviceName + "/" + this.version + "/" + this.fullStructName + ".htm"
            const self = this;
           $("#struct-model .struct-model-context").html(`
             <div class="row">
        <div class="col-sm-12 col-md-12">
            <div class="page-header mt5">
                <h1>${result.name}</h1>
            </div>
            <p data-marked-id="marked">${result.doc}</p>

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

            <h3>枚举成员</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>#</th>
                    <th>名称</th>
                    <th>值</th>
                    <th>描述</th>
                </tr>
                </thead>
                <tbody>
                ${self.enumItemsToHTML(result.enumItems)}
                </tbody>
            </table>

        </div>
    </div>
            `)
        }

        private enumItemsToHTML(enumItems){
            let enumItemsStr:String = "";
            for (let index = 0; index < enumItems.length; index++) {
                enumItemsStr += `
                               <tr>
                        <td>${index + 1}</td>
                        <td>${enumItems[index].label}</td>
                        <td>${enumItems[index].value}</td>
                        <td data-marked-id="marked">${enumItems[index].doc}</td>
                    </tr>
                            `
            }
            return enumItemsStr;
        }
    }
}