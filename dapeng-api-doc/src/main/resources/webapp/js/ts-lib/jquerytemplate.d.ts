/// <reference path="jquery.d.ts"/>

interface JQueryTemplateBindOptions {
    ignoreUndefined?: boolean;
    ignoreNull?: boolean;
    ignoreEmptyString?: boolean;
}

interface JQueryTemplateOptions {

    async?:boolean;

    overwriteCache?:boolean;

    complete?:Function;

    success?:Function;

    error?:Function;

    errorMessage?:string;

    paged?:boolean;

    pageNo?:number;

    elemPerPage?:number;

    append?:boolean;

    prepend?:boolean;

    beforeInsert?:Function;

    afterInsert?:Function;

    bindingOptions?:JQueryTemplateBindOptions;

}

interface JQuery {

    loadTemplate(template:JQuery, data:any, options?:JQueryTemplateOptions) :JQuery;

    loadTemplate(template:string, data:any, options?:JQueryTemplateOptions) :JQuery;

}

interface JQueryStatic {

    addTemplateFormatter(key:string, formatter:Function) :void;

}