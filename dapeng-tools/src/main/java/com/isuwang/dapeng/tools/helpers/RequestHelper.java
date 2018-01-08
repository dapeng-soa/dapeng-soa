package com.github.dapeng.tools.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.SoaSystemEnvProperties;
import com.github.dapeng.core.helper.SoaHeaderHelper;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.remoting.fake.json.JSONPost;
import com.github.dapeng.remoting.filter.LoadBalanceFilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Eric on 2016/2/15.
 */
public class RequestHelper {
    private static final String SERVICENAME = "serviceName";
    private static final String VERSION = "version";
    private static final String METHODNAME = "methodName";

    private static final String TAGSTART = "<%s>";
    private static final String TAGEND = "</%s>";
    private static final String TAGEMPTY = "<%s/>";


    private static JSONPost jsonPost;

    public static void post(String... args) {

        String jsonFile = checkArg(args);

        if (jsonFile == null) return;

        String jsonString = null;
        boolean isJson = false;
        if (jsonFile.endsWith(".json")) {
            isJson = true;
            jsonString = readFromeFile(jsonFile);
        } else if (jsonFile.endsWith(".xml")) {
            jsonString = parseFromXmlToJson(jsonFile);
        }

        JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        String parameter = jsonObject.get("params").toString();

        SoaHeader header = constructHeader(jsonObject);

        invokeService(header, parameter, isJson);
    }

    public static SoaHeader constructHeader(JsonObject jsonObject) {

        String serviceName = jsonObject.get(SERVICENAME).getAsString();
        String versionName = jsonObject.get(VERSION).getAsString();
        String methodName = jsonObject.get(METHODNAME).getAsString();
        SoaHeader header = SoaHeaderHelper.getSoaHeader(true);
        header.setServiceName(serviceName);
        header.setVersionName(versionName);
        header.setMethodName(methodName);
        header.setCallerFrom(Optional.of("dapeng-command"));
        header.setOperatorId(Optional.of(1));
        header.setOperatorName(Optional.of("快塑网"));
        return header;
    }

    private static String checkArg(String... args) {
        if (args.length != 2) {
            System.out.println("example: java -jar dapeng.jar request request.json");
            System.out.println("         java -jar dapeng.jar request request.xml");
            System.out.println("         java -Dsoa.service.ip=192.168.0.1 -Dsoa.service.port=9091 -jar dapeng.jar request request.json");
            System.out.println("         java -Dsoa.service.ip=192.168.0.1 -Dsoa.service.port=9091 -jar dapeng.jar request request.xml");
            System.exit(0);
        }
        String jsonFile = args[1];

        File requestFile = new File(jsonFile);
        if (!requestFile.exists()) {
            System.out.println("文件(" + requestFile + ")不存在");
            return null;
        }
        return jsonFile;
    }


    public static String invokeService(SoaHeader header, String parameter, boolean isJson) {

        System.out.println("Getting service from server...");
        Service service = ServiceCache.getService(header.getServiceName(), header.getVersionName());

        if (service == null) {
            System.out.println("无可用服务");
            return "{\"message\":\"无可用服务\"}";
        }

        System.out.println("Getting caller Info ...");
        String callerInfo = LoadBalanceFilter.getCallerInfo(header.getServiceName(), header.getVersionName(), header.getMethodName());

        if (callerInfo != null) {
            String[] infos = callerInfo.split(":");
            jsonPost = new JSONPost(infos[0], Integer.valueOf(infos[1]), true);

        } else if (SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local")) {
            jsonPost = new JSONPost(SoaSystemEnvProperties.SOA_SERVICE_IP, SoaSystemEnvProperties.SOA_SERVICE_PORT, true);

        } else {
            System.out.println("{\"message\":\"无可用服务\"}");
            return "{\"message\":\"无可用服务\"}";
        }

        try {
            System.out.println("Calling Service ...");

            String response = jsonPost.callServiceMethod(header, parameter, service);
            if (isJson) {
                System.out.println(response);
                return response;
            } else {
                JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
                StringBuffer xmlBuf = new StringBuffer();
                xmlBuf.append("<soaResponse>").append("\n");
                parseFromJsonToXml(xmlBuf, jsonObject);
                xmlBuf.append("</soaResponse>");
                printPrettyXml(xmlBuf);

                return xmlBuf.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"message\":\"" + e.getMessage() + "\"}";
        }

    }

    private static void printPrettyXml(StringBuffer xmlBuf) throws DocumentException, IOException {
        Document document = DocumentHelper.parseText(xmlBuf.toString());
        OutputFormat formater = OutputFormat.createPrettyPrint();
        formater.setEncoding("utf-8");
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, formater);
        writer.write(document);
        writer.close();
        System.out.println(out.toString());
    }

    private static void parseFromJsonToXml(StringBuffer sbxml, JsonObject jsonObject) {
        Set<Map.Entry<String, JsonElement>> set = jsonObject.entrySet();
        Iterator<Map.Entry<String, JsonElement>> iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> mJson = iterator.next();
            String tag = mJson.getKey();
            JsonElement innerEl = mJson.getValue();
            StringBuffer xmlTemp = new StringBuffer();
            if (innerEl.isJsonObject()) {
                xmlTemp.append(String.format(TAGSTART, tag)).append("\n");
                parseFromJsonToXml(xmlTemp, (JsonObject) innerEl);
                xmlTemp.append(String.format(TAGEND, tag));
                sbxml.append(xmlTemp);
            } else if (innerEl.isJsonArray()) {
                JsonArray innerElArray = ((JsonArray) innerEl);
                if (!tag.endsWith("s")) {
                    tag = tag + "s";
                }
                xmlTemp.append(String.format(TAGSTART, tag)).append("\n");
                for (int i = 0; i < innerElArray.size(); i++) {
                    xmlTemp.append(String.format(TAGSTART, tag.substring(0, tag.length() - 1))).append("\n");
                    JsonElement innerArrayEl = innerElArray.get(i);
                    parseFromJsonToXml(xmlTemp, (JsonObject) innerArrayEl);
                    xmlTemp.append(String.format(TAGEND, tag.substring(0, tag.length() - 1)));
                }
                xmlTemp.append(String.format(TAGEND, tag)).append("\n");
                sbxml.append(xmlTemp);
            } else if (innerEl.isJsonPrimitive()) {
                xmlTemp.append(String.format(TAGSTART + "%s" + TAGEND, tag, innerEl.getAsJsonPrimitive(), tag)).append("\n");
                sbxml.append(xmlTemp);
            } else if (innerEl.isJsonNull()) {
                xmlTemp.append(String.format(TAGEMPTY, tag));
            }
        }
    }

    private static String parseFromXmlToJson(String file) {
        String str = " ";
        try {
            SAXReader sax = new SAXReader();
            File xmlFile = new File(file);
            Document document = sax.read(xmlFile);
            Element root = document.getRootElement();
            str = getNodes(root);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return str.substring(0, str.length() - 1);
    }

    public static String getNodes(Element node) {
        StringBuffer sb = new StringBuffer();
        if (node.elements().size() == 0) {
            sb.append(String.format("\"%s\":%s,", node.getName(), "".equals(node.getTextTrim()) ? "{}" : node.getTextTrim()));
        } else {
            if (!node.isRootElement()) {
                sb.append(String.format("\"%s\":", node.getName()));
            }
            sb.append("{");

            List<Element> listElement = node.elements();
            for (int i = 0; i < listElement.size(); i++) {
                String temp = getNodes(listElement.get(i));
                if (i == listElement.size() - 1) {
                    sb.append(temp.substring(0, temp.length() - 1));
                } else {
                    sb.append(temp);
                }
            }
            sb.append("},");
        }
        return sb.toString();
    }

    private static String readFromeFile(String jsonFile) {
        StringBuilder sb = new StringBuilder();
        try {
            List<String> lines = Files.readAllLines(Paths.get(jsonFile), StandardCharsets.UTF_8);
            for (String line : lines) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
