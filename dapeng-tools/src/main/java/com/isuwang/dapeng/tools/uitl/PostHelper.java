package com.github.dapeng.tools.uitl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.tools.helpers.RequestHelper;

/**
 * Created by tangliu on 2016/9/13.
 */
public class PostHelper {
    /**
     * 根据json请求字符串，请求服务器，并获得返回结果
     *
     * @param jsonString
     * @return
     */
    public static String send(String jsonString) {
        JsonObject jsonObject;
        String parameter;
        SoaHeader header;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (Exception e) {
            System.out.println("json请求解析出错");
            e.printStackTrace();
            return "";
        }

        try {
            parameter = jsonObject.get("params").toString();
        } catch (Exception e) {
            System.out.println("params字段不存在");
            return "";
        }

        try {
            header = RequestHelper.constructHeader(jsonObject);
        } catch (Exception e) {
            System.out.println("soaHeader构造失败");
            e.printStackTrace();
            return "";
        }
        return RequestHelper.invokeService(header, parameter, true);
    }

    public static void main(String[] args) {
        String str = "{\n" +
                "  \"methodName\": \"sayHello\",\n" +
                "  \"serviceName\": \"com.github.dapeng.hello.service.HelloService\",\n" +
                "  \"params\": {\n" +
                "    \"name\": \"sampleDataString\"\n" +
                "  },\n" +
                "  \"version\": \"1.0.0\"\n" +
                "}";
        PostHelper.send(str);
    }

}
