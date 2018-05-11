<%--
  Created by IntelliJ IDEA.
  User: struy
  Date: 2018/2/22
  Time: 19:36
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--model--%>
<div id="struct-model" style="display: none">
    <div class="struct-model-container">
        <a class="back-to-previous" href="javascript:void(0)" onclick="backPreviousModle()"><span class="glyphicon glyphicon-arrow-left"></span>返回</a>
        <a class="model-close" title="关闭" onclick=structModelClose()><span class="glyphicon glyphicon-remove"></span></a>
        <div class="struct-model-context">
        </div>
    </div>
</div>

<script>
    window.$previousModle = [];
    function toggleOverflow() {
        $("body").css("overflow","hidden");
        if(window.$previousModle.length <= 1){
            $(".back-to-previous").hide();
        }
        setTimeout(function(){
            $("#struct-model").show();
            $("body").css("overflow","hidden");
            initStructModel();
        },100);
    }

    //结构体 弹出框
    function structModelClose() {
        window.$previousModle = [];
        $("#struct-model").hide();
        $("body").css("overflow","auto");
    }

    function initStructModel() {
        setTimeout(function () {
            $("#struct-model .struct-model-context").scrollTop(0);
        },10);
    }

    function getStructDetail(serviceName, serviceVersion, nameSpace, struct) {
        var sAction = new api.StructAction();
        sAction.findStruct(serviceName, serviceVersion, nameSpace + "." + struct, isModel = true);
        toggleOverflow();
    }

    function getStructDetail1(serviceName, serviceVersion, qualifiedName) {
        var sAction = new api.StructAction();

        sAction.findStruct(serviceName, serviceVersion, qualifiedName, isModel = true);
        toggleOverflow();
    }

    function getEnumDetail(serviceName, serviceVersion, nameSpace, enumName) {
        var eAction = new api.EnumAction();

        eAction.findEnum(serviceName, serviceVersion, nameSpace + "." + enumName, isModel = true);
        toggleOverflow();
    }

    function getEnumDetail1(serviceName, serviceVersion, qualifiedName) {
        var eAction = new api.EnumAction();

        eAction.findEnum(serviceName, serviceVersion,qualifiedName, isModel = true);
        toggleOverflow();
    }

    function openMethodDocDetail(obj) {
        var $content = $(obj).next();
        $("#struct-model .struct-model-context").html($content.html());
        toggleOverflow();
    }

    function backPreviousModle() {
        var maxIndex = window.$previousModle.length-1;
        maxIndex !== 1 ? void(0) : $(".back-to-previous").hide();
        if(maxIndex >= 1){
            window.$previousModle.pop();
            maxIndex = window.$previousModle.length-1;
            $("#struct-model .struct-model-context").html(window.$previousModle[maxIndex]);
        }
    }

</script>
