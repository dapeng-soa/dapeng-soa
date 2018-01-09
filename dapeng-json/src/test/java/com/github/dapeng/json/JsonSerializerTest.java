package com.github.dapeng.json;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TBinaryProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

/**
 * Unit test for simple App.
 */
public class JsonSerializerTest {

    public static void main(String[] args) throws IOException, TException {

        complexStructTest1();

        simpleStructTest();
        simpleMapTest();
        intArrayTest();
        intMapTest();
        enumTest();
        simpleStructWithEnumTest();
        simpleStructWithOptionTest();

        complexStructTest();
    }

    private static void complexStructTest() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml" ;
        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable")).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/orderService_createAppointmentForAvailable-complexStruct.json");

        String desc = "complexStructTest" ;
        doTest(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);

    }

    private static void complexStructTest1() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml" ;
        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable1")).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/complexStruct.json");

        String desc = "complexStructTest1" ;
        doTest(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);

    }

    private static void simpleStructTest() throws TException, IOException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml" ;
        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("payNotify")).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/orderService_payNotify.json");

        String desc = "simpleStructTest" ;
        doTest(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);
    }

    /**
     * Map<String,String>
     *
     * @throws IOException
     * @throws TException
     */
    private static void simpleMapTest() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml" ;
        Service orderService = getService(orderDescriptorXmlPath);

        Method method = orderService.methods.stream().filter(_method -> _method.name.equals("payNotifyForAlipay")).collect(Collectors.toList()).get(0);
        String json = loadJson("/orderService_payNotifyForAlipay-map.json");

        doTest(orderService, method, method.request, json, "simpleMapTest");
    }

    /**
     * Map<Integer, String>
     *
     * @throws IOException
     * @throws TException
     */
    private static void intMapTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml" ;

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_listDoctorsNameById-map.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("listDoctorsNameById")).collect(Collectors.toList()).get(0);

        doTest(crmService, method, method.response, json, "intMapTest");
    }

    private static void intArrayTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml" ;

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_listDoctorsNameById-list.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("listDoctorsNameById")).collect(Collectors.toList()).get(0);

        doTest(crmService, method, method.request, json, "intArrayTest");
    }

    private static void enumTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml" ;

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_modifyDoctorType-enum.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("modifyDoctorType")).collect(Collectors.toList()).get(0);

        doTest(crmService, method, method.request, json, "enumTest");
    }

    private static void simpleStructWithEnumTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml" ;

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_saveFocusDoctor-structWithEnum.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("saveFocusDoctor")).collect(Collectors.toList()).get(0);

        doTest(crmService, method, method.request, json, "simpleStructWithEnumTest");
    }

    private static void simpleStructWithOptionTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml" ;

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_getPatient-option.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("getPatient")).collect(Collectors.toList()).get(0);

        doTest(crmService, method, method.request, json, "simpleStructWithOptionTest");
    }


    private static void doTest(Service service, Method method, Struct struct, String json, String desc) throws TException {

//        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);
//
//        JsonSerializer jsonSerializer = new JsonSerializer(service, method, struct, requestBuf);
//
//        TProtocol outProtocol = new TBinaryProtocol(new TSoaTransport(requestBuf));
//        jsonSerializer.write(json, outProtocol);
//
//        TProtocol inProtocol = new TBinaryProtocol(new TSoaTransport(requestBuf));
//
//        System.out.println("origJson:\n" + json);
//
//        System.out.println("after enCode and decode:\n" + jsonSerializer.read(inProtocol));
//        System.out.println(desc + " ends=====================");

    }

    private static Service getService(final String xmlFilePath) throws IOException {
        String xmlContent = IOUtils.toString(JsonSerializerTest.class.getResource(xmlFilePath), "UTF-8");
        return JAXB.unmarshal(new StringReader(xmlContent), com.github.dapeng.core.metadata.Service.class);
    }

    private static String loadJson(final String jsonPath) throws IOException {
        return IOUtils.toString(JsonSerializerTest.class.getResource(jsonPath), "UTF-8");
    }
}
