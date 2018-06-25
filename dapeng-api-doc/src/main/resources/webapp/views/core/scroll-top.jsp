<%--
  Created by IntelliJ IDEA.
  User: struy
  Date: 2018/2/22
  Time: 19:28
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<a class="go-top" href="javascript:void(0)">
    <span>回顶部</span>
</a>
<script>
    /*返回顶部*/
    $(".go-top").click(function () {
        $("html,body").animate({scrollTop: 0}, "slow");
    });
</script>
