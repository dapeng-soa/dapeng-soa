function createInputGroup(label, type, optional, fieldDoc, input) {
    var $doc = document;

    var inputType = "text", placeholder = "类型：" + type, warn = "输入数据不是" + type + "类型";
    switch (type) {
        case "Integer":
            inputType = "number";
            break;
        case "Short":
            inputType = "number";
            break;
        case "Long":
            inputType = 'number';
            break;
        case "byte[]":
            placeholder = "类型：16进制字符串";
            break;
        default :
            inputType = "text";
            break;
    }

    var divElem = $doc.createElement("div");
    $(divElem).addClass("input-group");

    var labelElem = $doc.createElement("span");
    $(labelElem).addClass("input-group-addon");
    $(labelElem).addClass("parameterName");

    if (fieldDoc == null || fieldDoc == '') {
        fieldDoc = '暂无说明';
    }
    var attr = '<abbr title="' + fieldDoc + '">' + label + '</abbr>';
    $(labelElem).html(attr);

    $(labelElem).css("width", "125px");
    divElem.appendChild(labelElem);

    var inputElem = (input == undefined) ? $doc.createElement("input") : input;
    $(inputElem).addClass("form-control");
    $(inputElem).addClass("parameterValue");
    if(type == "Date"){
        $(inputElem).addClass("datetimepicker");
    }
    $(inputElem).attr("type", inputType);
    $(inputElem).attr("placeholder", placeholder);
    divElem.appendChild(inputElem);

    var spanElem = $doc.createElement("span");
    $(spanElem).addClass("input-group-addon");
    divElem.appendChild(spanElem);

    if (optional == true) {
        $(inputElem).attr("disabled", "disabled");

        var checkboxElem = $doc.createElement("input");
        $(checkboxElem).attr("type", "checkbox");
        checkboxElem.onclick = function () {
            if (this.checked) {
                $(inputElem).removeAttr("disabled")
            } else {
                $(inputElem).attr("disabled", "disabled");
                $(inputElem).val("");
            }
        }
        spanElem.appendChild(checkboxElem);
    }

    var warnElem = $doc.createElement("span");
    $(warnElem).html(warn);
    $(warnElem).addClass("warninfo");
    $(warnElem).css("display", "none");
    $(warnElem).css("color", "red");
    spanElem.appendChild(warnElem);

    var liElem = $doc.createElement("li");
    liElem.appendChild(divElem);
    $(liElem).css("padding", "1px 1px 1px 1px");

    return liElem;
}

function createSelector(values) {

    var select = document.createElement('select');

    for (var i = 0; i < values.length; i++) {
        var value = values[i];
        var option = document.createElement('option');
        option.value = value;
        option.innerHTML = value;
        select.appendChild(option);
    }
    return select;
}

function getDataTypeElement(dataType, name, service, optional, doc) {

    switch (dataType.kind) {

        case 'DATE':
            return createInputGroup(name, "Date", optional, doc);

        case 'BIGDECIMAL':
            return createInputGroup(name, "BigDecimal", optional, doc);

        case 'SHORT':
            return createInputGroup(name, 'Short', optional, doc);

        case 'STRING':
            return createInputGroup(name, "String", optional, doc);

        case 'INTEGER':
            return createInputGroup(name, "Integer", optional, doc);

        case 'DOUBLE':
            return createInputGroup(name, "Double", optional, doc);

        case 'BOOLEAN':
            var values = ['true', 'false'];
            var select = createSelector(values);
            return createInputGroup(name, 'Boolean', optional, doc, select);

        case 'BYTE':
            return createInputGroup(name, 'Byte', optional, doc);

        case 'BINARY':
            return createInputGroup(name, 'byte[]', optional, doc);


        case 'LONG':
            return createInputGroup(name, 'Long', optional, doc);

        case 'ENUM':
            var qualifiedName = dataType.qualifiedName;
            var enumName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
            var values = new Array();
            for (var index = 0; index < service.enumDefinitions.length; index++) {
                var tenum = service.enumDefinitions[index];
                if (tenum.name == enumName) {
                    for (var j = 0; j < tenum.enumItems.length; j++) {
                        var item = tenum.enumItems[j];
                        values[j] = item.label;
                    }
                }
            }
            return createInputGroup(name, "enum", optional, doc, createSelector(values));

        case 'MAP':
            var li = $("<li style='padding: 1px'><div class='input-group'><span class='input-group-addon parameterName'></span><input class='form-control' style='display: none'/><span class='input-group-addon'><button class='btn btn-success btn-xs'>+</button></span></div></li>");
            var span = li.find('span.parameterName');
            if (doc == null || doc == '') {
                doc = '暂无说明';
            }
            var attr = '<abbr title="' + doc + '">' + name + '(Map)</abbr>';
            span.html(attr);

            var addButton = li.find('button');

            addButton.type = 'button';
            addButton.click(function () {

                var targetUl = $(this).parent().parent().parent().children('ul');
                var li2 = $("<li style='padding: 1px'><div class='input-group'><span class='input-group-addon'>key-value</span><input class='form-control' style='display: none'/><span class='input-group-addon'><button class='btn btn-danger btn-xs'>-</button></span></div></li>");
                var delButton = li2.find('button.btn-danger');
                delButton.click(function () {
                        li2.remove()
                    }
                );

                var ul2 = document.createElement('ul');
                var li3 = getDataTypeElement(dataType.keyType, 'key', service, false, 'Key');
                var li4 = getDataTypeElement(dataType.valueType, 'value', service, false, 'Value');
                ul2.appendChild(li3)
                ul2.appendChild(li4);
                li2.append(ul2);

                targetUl.append(li2);
            });

            if (optional) {

                addButton.css('display', 'none');
                var checkBox = document.createElement('input');
                checkBox.type = 'checkbox';
                checkBox.className = 'checkbox';
                checkBox.style.display = 'inline';
                checkBox.style.marginLeft = '5px';
                checkBox.onclick = function () {

                    if (this.checked) {
                        addButton.css('display', '');
                    } else {
                        addButton.css('display', 'none');
                        li.children('ul').empty();
                    }
                };
                var span = li.find('span')[1];
                span.appendChild(checkBox);
            }
            var ul = document.createElement('ul');
            li.append(ul);
            return li[0];

        case 'LIST':

            var li = $("<li style='padding: 1px'><div class='input-group'><span class='input-group-addon parameterName'></span><input class='form-control' style='display: none'/><span class='input-group-addon'><button class='btn btn-success btn-xs'>+</button></span></div></li>");
            var span = li.find('span.parameterName');
            if (doc == null || doc == '') {
                doc = '暂无说明';
            }
            var attr = '<abbr title="' + doc + '">' + name + '(List)</abbr>';
            span.html(attr);

            var addButton = li.find('button');
            addButton.click(function () {

                var targetUl = $(this).parent().parent().parent().children('ul');
                var targetLi = getDataTypeElement(dataType.valueType, 'value', service, false, doc);

                var deleteButton = document.createElement('button');
                deleteButton.type = 'button';
                deleteButton.className = 'btn btn-danger btn-xs';
                deleteButton.innerHTML = '-'
                deleteButton.onclick = function () {
                    targetLi.remove();
                };
                $(targetLi).children('div').find('span')[1].appendChild(deleteButton);

                targetUl.append(targetLi);
            });

            if (optional) {

                addButton.css('display', 'none');
                var checkBox = document.createElement('input');
                checkBox.type = 'checkbox';
                checkBox.className = 'checkbox';
                checkBox.style.display = 'inline';
                checkBox.style.marginLeft = '5px';
                checkBox.onclick = function () {

                    if (this.checked) {
                        addButton.css('display', 'inline');
                    } else {
                        addButton.css('display', 'none');
                        $(li).children('ul').empty();
                    }
                };
                var span = li.find('span')[1];
                span.appendChild(checkBox);
            }

            var ul = document.createElement('ul');
            li.append(ul)
            return li[0];

        case 'SET':
            var li = $("<li style='padding: 1px'><div class='input-group'><span class='input-group-addon parameterName'></span><input class='form-control' style='display: none'/><span class='input-group-addon'><button class='btn btn-success btn-xs'>+</button></span></div></li>");
            var span = li.find('span.parameterName');
            if (doc == null || doc == '') {
                doc = '暂无说明';
            }
            var attr = '<abbr title="' + doc + '">' + name + '(Set)</abbr>';
            span.html(attr);

            var addButton = li.find('button');
            addButton.click(function () {

                var targetUl = $(this).parent().parent().parent().children('ul');
                var targetLi = getDataTypeElement(dataType.valueType, 'value', service);

                var deleteButton = document.createElement('button');
                deleteButton.type = 'button';
                deleteButton.className = 'btn btn-danger btn-xs';
                deleteButton.innerHTML = '-'
                deleteButton.onclick = function () {
                    targetLi.remove();
                };
                $(targetLi).children('div').find('span')[1].appendChild(deleteButton);

                targetUl.append(targetLi);
            });

            if (optional) {

                addButton.css('display', 'none');
                var checkBox = document.createElement('input');
                checkBox.type = 'checkbox';
                checkBox.className = 'checkbox';
                checkBox.style.display = 'inline';
                checkBox.style.marginLeft = '5px';
                checkBox.onclick = function () {

                    if (this.checked) {
                        addButton.css('display', 'inline');
                    } else {
                        addButton.css('display', 'none');
                        $(li).children('ul').empty();
                    }
                };
                var span = li.find('span')[1];
                span.appendChild(checkBox);
            }

            var ul = document.createElement('ul');
            li.append(ul);
            return li[0];

        case 'STRUCT':
            var li = $("<li><div class='input-group'><span class='input-group-addon parameterName'></span><input class='form-control' style='display: none'/><span class='input-group-addon'></span></div></li>");
            var span = li.find('span.parameterName');
            if (doc == null || doc == '') {
                doc = '暂无说明';
            }
            var attr = '<abbr title="' + doc + '">' + name + '(' + dataType.qualifiedName + ')</abbr>';
            span.html(attr);

            if (optional) {

                var checkSpan = li.find('span')[1];
                var checkBox = document.createElement('input');
                checkBox.type = 'checkbox';
                checkBox.className = 'checkbox';
                checkBox.style.display = 'inline';
                checkBox.onclick = function () {

                    if (this.checked) {
                        ul.style.display = 'block';
                    } else {
                        ul.style.display = 'none';
                    }
                };
                checkSpan.appendChild(checkBox);
            }

            var ul = document.createElement('ul');
            var qualifiedName = dataType.qualifiedName;
            var structName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
            for (var index = 0; index < service.structDefinitions.length; index++) {
                var struct = service.structDefinitions[index];
                if (struct.name == structName) {
                    for (var j = 0; j < struct.fields.length; j++) {
                        var field = struct.fields[j];
                        var li2 = getDataTypeElement(field.dataType, field.name, service, field.optional, field.doc);
                        ul.appendChild(li2);
                    }
                }
            }
            li.append(ul);

            if (optional) {
                ul.style.display = 'none';
            }
            return li[0];

        default:
            return '<li></li>';
    }
}

/**
 * 提交自定义json测试
 * @param serviceName
 * @param version
 * @param methodName
 */
function applyTestForJsonStr(serviceName, version, methodName){
    var params = $("#pasteJsonBox").val();
    var jsonObj = {};
    try {
        jsonObj = JSON.parse(params)
    }catch (e){
        alert("json格式异常请检查");
        return;
    }
    $("#json-request").html(getFormatedJsonHTML(jsonObj));
    var url = window.basePath + "/test.htm";
    $.post(url, {
        serviceName: serviceName,
        version: version,
        methodName: methodName,
        parameter: JSON.stringify(jsonObj)
    }, function (result) {
        $("#json-result").html(getFormatedJsonHTML(eval('(' + result + ')')));
    }, 'json');
}


function applyTest(serviceName, version, methodName) {

    inputError = false;
    var jsonParameter = getJsonParameter();
    if (inputError) {
        return;
    }

    $("#json-request").html(getFormatedJsonHTML(jsonParameter));

    var stringParameter = JSON.stringify(jsonParameter);
    var url = window.basePath + "/test.htm";
    $.post(url, {
        serviceName: serviceName,
        version: version,
        methodName: methodName,
        parameter: stringParameter
    }, function (result) {

        $("#json-result").html(getFormatedJsonHTML(eval('(' + result + ')')));

    }, 'json');
}

function getJsonParameter() {

    var parameter = {};

    $('#tree').children('li').each(function () {

        var tN = $(this).find('span.parameterName').children('abbr').html();
        if (tN.indexOf('(') > 0) {
            tN = tN.substring(0, tN.indexOf("("));
        }
        //过滤非必填且为选填的
        if ($(this).children().children().children("input[type='checkbox']").length > 0) {

            var checkbox = $(this).children().children().children("input[type='checkbox']")[0];
            if (checkbox.checked) {
                var tJSON = getJsonObject($(this));
                parameter[tN] = tJSON;
            }
        } else {
            var tJSON = getJsonObject($(this));
            parameter[tN] = tJSON;
        }
    });
    return {body:parameter};
}

function getJsonObject(li) {

    var name = $(li).find('span.parameterName').children('abbr').html();

    if (name.indexOf('(Map)') > 0) {

        var map = {};
        var ul = $(li).children('ul');
        $(ul).children('li').each(function () {

            var ul2 = $(this).children('ul');
            var li_key = $(ul2).children('li')[0];
            var li_val = $(ul2).children('li')[1];

            var tN = $(li_key).find('input').val();
            map[tN] = getJsonObject(li_val);

        });
        return map;

    } else if (name.indexOf('(List)') > 0) {

        var list = [];
        var ul = $(li).children('ul');
        $(ul).children('li').each(function () {
            list.push(getJsonObject($(this)));
        });
        return list;

    } else if (name.indexOf('(Set)') > 0) {

        var aSet = [];
        var ul = $(li).children('ul');
        $(ul).children('li').each(function () {
            aSet.push(getJsonObject($(this)));
        });

        return aSet;

    } else if (name.indexOf('(') > 0) {

        var struct = {};
        var ul = $(li).children('ul');
        $(ul).children('li').each(function () {

            var tN = $(this).find('span.parameterName').children('abbr').html();
            if (tN.indexOf('(') > 0) {
                tN = tN.substring(0, tN.indexOf("("));
            }
            if ($(this).children().children().children("input[type='checkbox']").length > 0) {

                var checkbox = $(this).children().children().children("input[type='checkbox']")[0];
                if (checkbox.checked) {
                    struct[tN] = getJsonObject($(this));
                }
            } else {
                struct[tN] = getJsonObject($(this));
            }

        });
        return struct;

    } else if ($(li).find('select').length > 0) {

        return $(li).find('select').val();

    } else {

        var v = $(li).find('input')[0].value.trim();

        if ($(li).find('input')[0].type == 'number') {

            if (v == '') {
                $(li).find('span.warninfo').css("display", "");
                inputError = true;
            } else {
                $(li).find('span.warninfo').css("display", "none");
                return parseInt(v);
            }
        } else {

            if (v == '') {
                $(li).find('span.warninfo').html('输入内容不能为空');
                $(li).find('span.warninfo').css("display", "");
                inputError = true;
            } else {
                $(li).find('span.warninfo').css("display", "none");
            }
        }

        return $(li).find('input').val();
    }
}

var inputError = false;

function getJsonSample(dataType, service) {

    switch (dataType.kind) {
        case 'STRING':
            return "sampleDataString";
        case 'INTEGER':
            return Math.round(Math.random() * 1000);
        case 'DOUBLE':
            return Math.random() * 100;
        case 'BOOLEAN':
            return Math.round(Math.random()) == 1 ? "true" : "false";
        case 'BYTE':
            return parseInt(Math.random() * 256 - 128).toString;
        case 'BINARY':
            return "546869732049732041205465737420427974652041727261792E";
        case 'Short':
            return Math.round(Math.random() * 100);
        case 'LONG':
            return Math.round(Math.random() * 1000);
        case 'ENUM':
            for (var i = 0; i < service.enumDefinitions.length; i++) {

                var tenum = service.enumDefinitions[i];
                if ((tenum.namespace + "." + tenum.name) == dataType.qualifiedName) {
                    var size = tenum.enumItems.length;
                    var index = parseInt(Math.random() * size);
                    return tenum.enumItems[index].label;
                }
            }
            return "";
        case 'MAP':
            var map = {};
            var key = getJsonSample(dataType.keyType, service);
            var value = getJsonSample(dataType.valueType, service);
            map[key] = value;
            return map;
        case 'LIST':
            var list = [];
            list.push(getJsonSample(dataType.valueType, service));
            list.push(getJsonSample(dataType.valueType, service));
            return list;
        case 'SET':
            var list = [];
            list.push(getJsonSample(dataType.valueType, service));
            list.push(getJsonSample(dataType.valueType, service));
            return list;
        case 'STRUCT':
            var p = {};
            for (var i = 0; i < service.structDefinitions.length; i++) {
                var struct = service.structDefinitions[i];
                if ((struct.namespace + '.' + struct.name) == dataType.qualifiedName) {
                    for (var index = 0; index < struct.fields.length; index++) {
                        var field = struct.fields[index];
                        p[field.name] = getJsonSample(field.dataType, service);
                    }
                    return p;
                }
            }
            return {};

        case 'DATE':
            return "2016/04/13 16:00";
        case 'BIGDECIMAL':
            return "1234567.123456789123456";
        default :
            return "";
    }

}