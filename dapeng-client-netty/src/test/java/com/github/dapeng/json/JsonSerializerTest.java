package com.github.dapeng.json;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.org.apache.thrift.protocol.TBinaryProtocol;
import com.github.dapeng.org.apache.thrift.protocol.TProtocol;
import com.github.dapeng.util.DumpUtil;
import com.github.dapeng.util.SoaJsonMessageBuilder;
import com.github.dapeng.util.SoaMessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Unit test for simple App.
 */
public class JsonSerializerTest {

    public static void main(String[] args) throws IOException, TException {

        createSupplierTest();
        redundancyTest();
//        optionalBooleanTest();
//        simpleStructTest();
//        simpleMapTest();
//        intArrayTest();
//        intMapTest();
//        enumTest();
//        simpleStructWithEnumTest();
//        simpleStructWithOptionTest();
//
//        complexStructTest();
//        complexStructTest1();

    }

    private static void createSupplierTest() throws IOException, TException {
        final String supplierDescriptorXmlPath = "/com.today.api.supplier.service.SupplierService.xml";
        Service orderService = getService(supplierDescriptorXmlPath);

        Method createSupplier = orderService.methods.stream().filter(method -> method.name.equals("createSupplier")).collect(Collectors.toList()).get(0);
        String json = loadJson("/createSupplier.json");

        String desc = "createSupplierTest";
        doTest2(orderService, createSupplier, createSupplier.request, json, desc);
    }

    /**
     * 多余属性处理
     * @throws IOException
     */
    private static void redundancyTest() throws IOException, TException {
        final String categoryDescriptorXmlPath = "/com.today.api.category.service.CategoryService.xml";
        Service categoryService = getService(categoryDescriptorXmlPath);

        Method createCategoryAttribute = categoryService.methods.stream().filter(method -> method.name.equals("createCategoryAttribute")).collect(Collectors.toList()).get(0);
        String json = loadJson("/categoryService_createCategoryAttribute.json");

        String desc = "redundancyTest";
        doTest2(categoryService, createCategoryAttribute, createCategoryAttribute.request, json, desc);

    }

    private static void optionalBooleanTest() throws IOException, TException {
        final String supplierDescriptorXmlPath = "/com.today.api.supplier.service.SupplierService.xml";
        Service orderService = getService(supplierDescriptorXmlPath);

        Method createSupplierToGoods = orderService.methods.stream().filter(method -> method.name.equals("createSupplierToGoods")).collect(Collectors.toList()).get(0);
        String json = loadJson("/supplierService_optionalBooleanStruct.json");

        String desc = "optionalBooleanTest";
        doTest2(orderService, createSupplierToGoods, createSupplierToGoods.request, json, desc);

    }

    private static void complexStructTest() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable")).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/orderService_createAppointmentForAvailable-complexStruct.json");

        String desc = "complexStructTest";
        doTest2(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);

    }

    private static void complexStructTest1() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable1")).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/complexStruct.json");

        String desc = "complexStructTest1";
        doTest2(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);

    }

    private static void simpleStructTest() throws TException, IOException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        Service orderService = getService(orderDescriptorXmlPath);

        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("payNotify")).collect(Collectors.toList()).get(0);
        String payNotifyJson = loadJson("/orderService_payNotify.json");

        String desc = "simpleStructTest";
        doTest2(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);
    }

    /**
     * Map<String,String>
     *
     * @throws IOException
     * @throws TException
     */
    private static void simpleMapTest() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        Service orderService = getService(orderDescriptorXmlPath);

        Method method = orderService.methods.stream().filter(_method -> _method.name.equals("payNotifyForAlipay")).collect(Collectors.toList()).get(0);
        String json = loadJson("/orderService_payNotifyForAlipay-map.json");

        doTest2(orderService, method, method.request, json, "simpleMapTest");
    }

    /**
     * Map<Integer, String>
     *
     * @throws IOException
     * @throws TException
     */
    private static void intMapTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_listDoctorsNameById-map.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("listDoctorsNameById")).collect(Collectors.toList()).get(0);

        doTest2(crmService, method, method.response, json, "intMapTest");
    }

    private static void intArrayTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_listDoctorsNameById-list.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("listDoctorsNameById")).collect(Collectors.toList()).get(0);

        doTest2(crmService, method, method.request, json, "intArrayTest");
    }

    private static void enumTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_modifyDoctorType-enum.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("modifyDoctorType")).collect(Collectors.toList()).get(0);

        doTest2(crmService, method, method.request, json, "enumTest");
    }

    private static void simpleStructWithEnumTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_saveFocusDoctor-structWithEnum.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("saveFocusDoctor")).collect(Collectors.toList()).get(0);

        doTest2(crmService, method, method.request, json, "simpleStructWithEnumTest");
    }

    private static void simpleStructWithOptionTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        Service crmService = getService(crmDescriptorXmlPath);

        String json = loadJson("/crmService_getPatient-option.json");

        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("getPatient")).collect(Collectors.toList()).get(0);

        doTest2(crmService, method, method.request, json, "simpleStructWithOptionTest");
    }

    private static void doTest2(Service service, Method method, Struct struct, String json, String desc) throws TException {

        InvocationContext invocationContext = InvocationContextImpl.Factory.createNewInstance();
        invocationContext.setCodecProtocol(CodecProtocol.Binary);

        invocationContext.setServiceName(service.name);
        invocationContext.setVersionName(service.meta.version);
        invocationContext.setMethodName(method.name);
        invocationContext.setCallerFrom(Optional.of("JsonCaller"));

        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);

        JsonSerializer jsonSerializer = new JsonSerializer(service, method, struct);

        SoaJsonMessageBuilder<String> builder = new SoaJsonMessageBuilder();

        jsonSerializer.setRequestByteBuf(requestBuf);

        ByteBuf buf = builder.buffer(requestBuf)
                .body(json, jsonSerializer)
                .seqid(10)
                .build();
        System.out.println("origJson:\n" + json);



        DumpUtil.dump(buf);

        JsonSerializer jsonDecoder = new JsonSerializer(service, method, struct);

        SoaMessageParser<String> parser = new SoaMessageParser<>(buf, jsonDecoder);
        parser.parseHeader();

        System.out.println("after enCode and decode:\n" + parser.parseBody().getBody());
        System.out.println(desc + " ends=====================");
        requestBuf.release();
        InvocationContextImpl.Factory.removeCurrentInstance();
    }

    private static Service getService(final String xmlFilePath) throws IOException {
        String xmlContent = IOUtils.toString(JsonSerializerTest.class.getResource(xmlFilePath), "UTF-8");
        return JAXB.unmarshal(new StringReader(xmlContent), Service.class);
    }

    private static String loadJson(final String jsonPath) throws IOException {
        return IOUtils.toString(JsonSerializerTest.class.getResource(jsonPath), "UTF-8");
    }
}
