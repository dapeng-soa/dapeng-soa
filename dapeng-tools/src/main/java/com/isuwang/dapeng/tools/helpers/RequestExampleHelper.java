package com.github.dapeng.tools.helpers;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.github.dapeng.core.metadata.*;
import net.sf.json.JSONArray;
import net.sf.json.xml.XMLSerializer;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Eric on 2016/2/16.
 */
public class RequestExampleHelper {

    public static void getRequestJson(String... args) {
        if (args.length != 4) {
            System.out.println("example: java -jar dapeng.jar json com.github.dapeng.soa.hello.service.HelloService 1.0.0 sayHello");
            System.exit(0);
        }
        String serviceName = args[1];
        String versionName = args[2];
        String methodName = args[3];

        System.out.println("Getting Service metadata ...");
        Service service = getService(serviceName, versionName, methodName);
        List<Struct> structs = getMethod(service, methodName);
        for (Struct struct : structs) {
            System.out.println("---------------------------------------------------------------");
            List<Field> parameters = struct.getFields();
            Map<String, Object>  map = new HashMap<>();
            map.put("serviceName",serviceName);
            map.put("version",versionName);
            map.put("methodName",methodName);
            map.put("params",getSample(service, parameters));
            System.out.println(gson_format.toJson(map));
        }
    }


    public static void getRequestXml(String... args) {
        if (args.length != 4) {
            System.out.println("example: java -jar dapeng.jar xml com.github.dapeng.soa.hello.service.HelloService 1.0.0 sayHello");
            System.exit(0);
        }
        String serviceName = args[1];
        String versionName = args[2];
        String methodName = args[3];

        System.out.println("Getting Service metadata ...");
        Service service = getService(serviceName, versionName, methodName);
        List<Struct> structs = getMethod(service, methodName);
        List<Map<String, Object>> lists = new ArrayList<Map<String, Object>>();
        for (Struct struct : structs) {
            System.out.println("---------------------------------------------------------------");
            List<Field> parameters = struct.getFields();
            Map<String, Object> jsonSample = getSample(service, parameters);
            lists.add(jsonSample);
        }

        XMLSerializer xmlserializer = new XMLSerializer();
        JSONArray jobj = JSONArray.fromObject(gson_format.toJson(lists));

        String xmlStr = (xmlserializer.write(jobj));

        printXmlPretty(xmlStr,serviceName, versionName, methodName);
    }

    private static void printXmlPretty(String xmlStr,String serviceName, String versionName,String methodName) {
        try {
            SAXReader sax = new SAXReader();
            org.dom4j.Document document = sax.read(new StringReader(xmlStr));
            org.dom4j.Element root = document.getRootElement();
            List<Element> listElement = root.elements();
            StringBuffer xmlBuf = new StringBuffer();
            xmlBuf.append("<soaXmlRequest>").append("\n");
            xmlBuf.append(String.format("<serviceName>%s</serviceName>",serviceName));
            xmlBuf.append(String.format("<methodName>%s</methodName>",methodName));
            xmlBuf.append(String.format("<version>%s</version>",versionName));
            xmlBuf.append(String.format("<params>"));
            if (listElement.size() >= 1) {
                printNodes(listElement.get(0), xmlBuf);
            }
            xmlBuf.append(String.format("</params>"));
            xmlBuf.append("</soaXmlRequest>");
            Document documentResult = DocumentHelper.parseText(xmlBuf.toString());
            OutputFormat formater = OutputFormat.createPrettyPrint();
            formater.setEncoding("utf-8");
            StringWriter out = new StringWriter();
            XMLWriter writer = new XMLWriter(out, formater);
            writer.write(documentResult);
            writer.close();
            System.out.println(out.toString());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getLocalizedMessage());
        }
    }

    public static void printNodes(Element node, StringBuffer xmlBuf) {
        if (node.elements().size() == 0) {
            if (!"e".equals(node.getName()))
            xmlBuf.append(String.format("<%s>%s</%s>", node.getName(), node.getTextTrim(), node.getName())).append("\n");
        } else {
            if (!"e".equals(node.getName()))
                xmlBuf.append(String.format("<%s>", node.getName())).append("\n");

            if ("array".equals(node.attributeValue("class"))) {
                List<Element> arrayElements = node.elements();
                for (int i = 0; i < arrayElements.size(); i++) {
                    Element arrayElement = arrayElements.get(i);
                    String attValue = arrayElement.attributeValue("type");
                    if ("e".equals(arrayElement.getName()) && ("string".equals(attValue) || "number".equals(attValue))) {
                        if ("string".equals(attValue)) {
                            if (i != arrayElements.size() - 1) {
                                xmlBuf.append(String.format("\"%s\",", arrayElement.getTextTrim()));
                            } else {
                                xmlBuf.append(String.format("\"%s\"", arrayElement.getTextTrim())).append("\n");
                            }
                        } else if ("number".equals(attValue)) {
                            if (i != arrayElements.size() - 1) {
                                xmlBuf.append(String.format("%s,", arrayElement.getTextTrim()));
                            } else {
                                xmlBuf.append(String.format("%s", arrayElement.getTextTrim())).append("\n");
                            }
                        }

                    }
                }
            } else {
                List<Element> listElement = node.elements();
                Element currentElement = null;
                for (int i = 0; i < listElement.size(); i++) {
                    currentElement = listElement.get(i);
                    printNodes(currentElement, xmlBuf);
                }
            }

            if (!"e".equals(node.getName()))
                xmlBuf.append(String.format("</%s>", node.getName())).append("\n");
        }


    }


    private static Service getService(String serviceName, String versionName, String methodName) {

        Service service = ServiceCache.getService(serviceName, versionName);
        return service;
    }

    private static List<Struct> getMethod(Service service, String methodName) {
        List<Struct> structs = new ArrayList<Struct>();
        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                structs.add(method.getRequest());
            }
        }
        return structs;
    }


    private final static Gson gson_format = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("attachment") || f.getName().equals("__isset_bitfield");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return clazz == ByteBuffer.class;
        }
    }).setPrettyPrinting().create();

    private static Map<String, Object> getSample(Service service, List<Field> parameters) {
        String fieldName;
        DataType fieldType;
        Map<String, Object> mapTemp = new HashMap<String, Object>();
        for (int i = 0; i < parameters.size(); i++) {
            Field parameter = parameters.get(i);
            fieldName = parameter.getName();
            fieldType = parameter.getDataType();
            mapTemp.put(fieldName, assignValue(service, fieldType));
        }
        return mapTemp;
    }

    private static Object assignValue(Service service, DataType fieldType) {
        Object randomValue = null;
        switch (fieldType.getKind()) {
            case VOID:
                break;
            case BOOLEAN:
                randomValue = Math.round(Math.random()) == 1 ? "true" : "false";
                break;
            case BYTE:
                randomValue = (byte) (Math.random() * 256 - 128);
                break;
            case SHORT:
                randomValue = Math.round(Math.random() * 100);
                break;
            case INTEGER:
                randomValue = Math.round(Math.random() * 1000);
                break;
            case LONG:
                randomValue = Math.round(Math.random() * 1000);
                break;
            case DOUBLE:
                randomValue = Math.random() * 100;
                break;
            case STRING:
                randomValue = "sampleDataString";
                break;
            case BINARY:
                randomValue = "546869732049732041205465737420427974652041727261792E";
                break;
            case MAP:
                DataType keyType = fieldType.getKeyType();
                DataType valueType = fieldType.getValueType();
                Map<Object, Object> mapTemp = new HashMap<Object, Object>();
                Object key = assignValue(service, keyType);
                Object value = assignValue(service, valueType);
                mapTemp.put(key, value);

                randomValue = mapTemp;
                break;
            case LIST:
                List list = new ArrayList<Object>();
                DataType listValueType = fieldType.getValueType();
                list.add(assignValue(service, listValueType));
                list.add(assignValue(service, listValueType));

                randomValue = list;
                break;
            case SET:
                Set set = new HashSet<Object>();
                DataType setValueType = fieldType.getValueType();
                set.add(assignValue(service, setValueType));
                set.add(assignValue(service, setValueType));
                randomValue = set;
                break;
            case ENUM:
                List<TEnum> structsE = service.getEnumDefinitions();
                for (int i = 0; i < structsE.size(); i++) {
                    TEnum tenum = structsE.get(i);
                    if ((tenum.getNamespace() + "." + tenum.getName()) == fieldType.qualifiedName) {
                        int size = tenum.enumItems.size();
                        int index = (int) (Math.random() * size);
                        return tenum.enumItems.get(index).label;
                    }
                }
                return "";
            case STRUCT:
                List<Struct> structs = service.getStructDefinitions();
                for (int i = 0; i < structs.size(); i++) {
                    Struct struct = structs.get(i);
                    if ((struct.getNamespace() + '.' + struct.getName()).equals(fieldType.getQualifiedName())) {
                        randomValue = getSample(service, struct.getFields());
                    }
                }
                break;

            case DATE:
                randomValue = "2016/06/16 16:00";
                break;
            case BIGDECIMAL:
                randomValue = "1234567.123456789123456";
                break;
            default:
                randomValue = "";
        }
        return randomValue;
    }
}