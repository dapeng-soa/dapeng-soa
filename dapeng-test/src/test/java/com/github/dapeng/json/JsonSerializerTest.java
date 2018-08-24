package com.github.dapeng.json;

import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaMessageBuilder;
import com.github.dapeng.util.SoaMessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.PreferHeapByteBufAllocator;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.dapeng.util.DumpUtil.dumpToStr;
import static com.github.dapeng.util.DumpUtil.hexStr2bytes;

/**
 * Unit test for simple App.
 */
public class JsonSerializerTest {
    static  long t1 = 0;
    static  long t2 = 0;
    public static void main(String[] args) throws InterruptedException, TException, IOException {
        String result = "{\"name\":\"Ever\",\"success\":124, \"desc\":\"static void main(String[] args) throws InterruptedException, TException, IOException\", \"subEle\":{\"id\":2,\"age\":13}}";

//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("payNotify")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/orderService_payNotify.json");
//
//        String desc = "simpleStructTest";

//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable1")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/complexStruct.json");
//
//        String desc = "complexStructTest1";


        final String purchaseDescriptorXmlPath = "/com.today.api.purchase.service.PurchaseService.xml";
        Service purchaseService = getService(purchaseDescriptorXmlPath);

        Method createTransferOrder = purchaseService.methods.stream().filter(method -> method.name.equals("createTransferOrder")).collect(Collectors.toList()).get(0);
        String json = loadJson("/createTransferOrder.json");

        String desc = "createTransferOrderTest.json";

//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/orderService_createAppointmentForAvailable-complexStruct.json");
//
//        String desc = "complexStructTest";

        OptimizedMetadata.OptimizedService optimizedService = new OptimizedMetadata.OptimizedService(purchaseService);

        OptimizedMetadata.OptimizedStruct optimizedStruct = new OptimizedMetadata.OptimizedStruct(createTransferOrder.request);
        for (int i = 0; i < 100000; i++) {
//                String resulta = result.equals("{}") ? "{\"status\":1}" : result.substring(0, result.lastIndexOf('}')) + ",\"status\":1}";
//            doTest2(optimizedService, orderServicePayNotify, optimizedStruct, payNotifyJson, desc);
                doTest2(optimizedService, createTransferOrder, optimizedStruct, json, desc);
        }

        t1 = 0;
        t2 = 0;

        long cost = 0;
        int round = 5;
        for (int j = 0; j < round; j++) {
            long t11 = System.nanoTime();
            for (int i = 0; i < 100000; i++) {
//                String resulta = result.equals("{}") ? "{\"status\":1}" : result.substring(0, result.lastIndexOf('}')) + ",\"status\":1}";
//                doTest2(optimizedService, orderServicePayNotify, optimizedStruct, payNotifyJson, desc);
                doTest2(optimizedService, createTransferOrder, optimizedStruct, json, desc);
            }
            long t22 = System.nanoTime() - t11;
            System.out.println("cost:" + t22/1000000);
            cost += t22;
//            Thread.sleep(200);
        }
        System.out.println("average:" + cost/round/1000000);
        System.out.println("average:" + t1/round/1000000);
        System.out.println("average:" + t2/round/1000000);

//        createTransferOrderTest();
//        optionalBooleanTest();
//        simpleStructTest();
//        simpleMapTest();
//        createTransferOrderTest();
//        intArrayTest();
//        intMapTest();
//        enumTest();
//        simpleStructWithEnumTest();
//        simpleStructWithOptionTest();
//
//        complexStructTest();
//        complexStructTest1();
    }
//
//    private static void concurrentTest() throws InterruptedException {
//        long begin = System.currentTimeMillis();
//        Executor ec = Executors.newFixedThreadPool(5);
//        for (int i = 0; i < 10000; i++) {
//            ec.execute(() -> {
//                try {
//                    listCategoryDetailBySkuNosTest();
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                    System.exit(-1);
//                }
//            });
//            ec.execute(() -> {
//                try {
//                    listSkuDetailBySkuNosTest();
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                    System.exit(-1);
//                }
//            });
//        }
//        ((ExecutorService) ec).awaitTermination(100, TimeUnit.HOURS);
//        System.out.println("end:" + (System.currentTimeMillis() - begin));
//    }
//
//    private static void createTransferOrderTest() throws IOException, TException {
//        final String purchaseDescriptorXmlPath = "/com.today.api.purchase.service.PurchaseService.xml";
//        Service purchaseService = getService(purchaseDescriptorXmlPath);
//
//        Method createTransferOrder = purchaseService.methods.stream().filter(method -> method.name.equals("createTransferOrder")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/createTransferOrder.json");
//
//        String desc = "createTransferOrderTest.json";
//        doTest2(purchaseService, createTransferOrder, createTransferOrder.request, json, desc);
//
//    }
//
//    /**
//     * 多余属性处理
//     *
//     * @throws IOException
//     */
//    private static void redundancyTest() throws IOException, TException {
//        final String categoryDescriptorXmlPath = "/com.today.api.category.service.CategoryService.xml";
//        Service categoryService = getService(categoryDescriptorXmlPath);
//
//        Method createCategoryAttribute = categoryService.methods.stream().filter(method -> method.name.equals("createCategoryAttribute")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/categoryService_createCategoryAttribute.json");
//
//        String desc = "redundancyTest";
//        doTest2(categoryService, createCategoryAttribute, createCategoryAttribute.request, json, desc);
//
//    }
//
//    private static void listCategoryDetailBySkuNosTest() throws IOException, TException {
//        final String categoryDescriptorXmlPath = "/com.today.api.category.service.OpenCategoryService.xml";
//        Service categoryService = getService(categoryDescriptorXmlPath);
//
//        Method createCategoryAttribute = categoryService.methods.stream().filter(method -> method.name.equals("listCategoryDetailBySkuNos")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/listCategoryDetailBySkuNos.json");
//
//        String desc = "listCategoryDetailBySkuNosTest";
//        doTest2(categoryService, createCategoryAttribute, createCategoryAttribute.request, json, desc);
//
//    }
//
//    private static void listSkuDetailBySkuNosTest() throws IOException, TException {
//        final String categoryDescriptorXmlPath = "/com.today.api.goods.service.OpenGoodsService.xml";
//        Service categoryService = getService(categoryDescriptorXmlPath);
//
//        Method createCategoryAttribute = categoryService.methods.stream().filter(method -> method.name.equals("listSkuDetailBySkuNos")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/listSkuDetailBySkuNos.json");
//
//        String desc = "listSkuDetailBySkuNosTest";
//        doTest2(categoryService, createCategoryAttribute, createCategoryAttribute.request, json, desc);
//
//    }
//
//
//    private static void optionalBooleanTest() throws IOException, TException {
//        final String supplierDescriptorXmlPath = "/com.today.api.supplier.service.SupplierService.xml";
//        Service orderService = getService(supplierDescriptorXmlPath);
//
//        Method createSupplierToGoods = orderService.methods.stream().filter(method -> method.name.equals("createSupplierToGoods")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/supplierService_optionalBooleanStruct.json");
//
//        String desc = "optionalBooleanTest";
//        doTest2(orderService, createSupplierToGoods, createSupplierToGoods.request, json, desc);
//
//    }
//
//    private static void complexStructTest() throws IOException, TException {
//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/orderService_createAppointmentForAvailable-complexStruct.json");
//
//        String desc = "complexStructTest";
//        doTest2(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);
//
//    }
//
//    private static void complexStructTest1() throws IOException, TException {
//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable1")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/complexStruct.json");
//
//        String desc = "complexStructTest1";
//        doTest2(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);
//
//    }
//
//    private static void simpleStructTest() throws TException, IOException {
//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("payNotify")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/orderService_payNotify.json");
//
//        String desc = "simpleStructTest";
//        doTest2(orderService, orderServicePayNotify, orderServicePayNotify.request, payNotifyJson, desc);
//    }
//
//    /**
//     * Map<String,String>
//     *
//     * @throws IOException
//     * @throws TException
//     */
//    private static void simpleMapTest() throws IOException, TException {
//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method method = orderService.methods.stream().filter(_method -> _method.name.equals("payNotifyForAlipay")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/orderService_payNotifyForAlipay-map.json");
//
//        doTest2(orderService, method, method.request, json, "simpleMapTest");
//    }
//
//    /**
//     * Map<Integer, String>
//     *
//     * @throws IOException
//     * @throws TException
//     */
//    private static void intMapTest() throws IOException, TException {
//        final String crmDescriptorXmlPath = "/crm.xml";
//
//        Service crmService = getService(crmDescriptorXmlPath);
//
//        String json = loadJson("/crmService_listDoctorsNameById-map.json");
//
//        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("listDoctorsNameById")).collect(Collectors.toList()).get(0);
//
//        doTest2(crmService, method, method.response, json, "intMapTest");
//    }
//
//    private static void intArrayTest() throws IOException, TException {
//        final String crmDescriptorXmlPath = "/crm.xml";
//
//        Service crmService = getService(crmDescriptorXmlPath);
//
//        String json = loadJson("/crmService_listDoctorsNameById-list.json");
//
//        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("listDoctorsNameById")).collect(Collectors.toList()).get(0);
//
//        doTest2(crmService, method, method.request, json, "intArrayTest");
//    }
//
//    private static void enumTest() throws IOException, TException {
//        final String crmDescriptorXmlPath = "/crm.xml";
//
//        Service crmService = getService(crmDescriptorXmlPath);
//
//        String json = loadJson("/crmService_modifyDoctorType-enum.json");
//
//        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("modifyDoctorType")).collect(Collectors.toList()).get(0);
//
//        doTest2(crmService, method, method.request, json, "enumTest");
//    }
//
//    private static void simpleStructWithEnumTest() throws IOException, TException {
//        final String crmDescriptorXmlPath = "/crm.xml";
//
//        Service crmService = getService(crmDescriptorXmlPath);
//
//        String json = loadJson("/crmService_saveFocusDoctor-structWithEnum.json");
//
//        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("saveFocusDoctor")).collect(Collectors.toList()).get(0);
//
//        doTest2(crmService, method, method.request, json, "simpleStructWithEnumTest");
//    }
//
//    private static void simpleStructWithOptionTest() throws IOException, TException {
//        final String crmDescriptorXmlPath = "/crm.xml";
//
//        Service crmService = getService(crmDescriptorXmlPath);
//
//        String json = loadJson("/crmService_getPatient-option.json");
//
//        Method method = crmService.methods.stream().filter(_method -> _method.name.equals("getPatient")).collect(Collectors.toList()).get(0);
//
//        doTest2(crmService, method, method.request, json, "simpleStructWithOptionTest");
//    }

    private static void doTest2(OptimizedMetadata.OptimizedService optimizedServicee, Method method, OptimizedMetadata.OptimizedStruct optimizedStruct, String json, String desc) throws TException {
        long begin = System.nanoTime();
        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.createNewInstance();
        invocationContext.codecProtocol(CodecProtocol.CompressedBinary);

        invocationContext.serviceName(optimizedServicee.service.name);
        invocationContext.versionName(optimizedServicee.service.meta.version);
        invocationContext.methodName(method.name);
        invocationContext.callerMid("JsonCaller");

        final ByteBuf requestBuf = PreferHeapByteBufAllocator.DEFAULT.buffer(8192);


        JsonSerializer jsonSerializer = new JsonSerializer(optimizedServicee, method, "1.0.0", optimizedStruct);

        SoaMessageBuilder<String> builder = new SoaMessageBuilder();

        jsonSerializer.setRequestByteBuf(requestBuf);

        ByteBuf buf = builder.buffer(requestBuf)
                .body(json, jsonSerializer)
                .seqid(10)
                .build();
//        System.out.println("origJson:\n" + json);
//
//
//        System.out.println(dumpToStr(buf));

        long middle = System.nanoTime();

        JsonSerializer jsonDecoder = new JsonSerializer(optimizedServicee, method, "1.0.0", optimizedStruct);

        SoaMessageParser<String> parser = new SoaMessageParser<>(buf, jsonDecoder);
        parser.parseHeader();
//        parser.getHeader();
//        parser.parseBody();
//System.out.println(parser.getHeader());
        System.out.println("after enCode and decode:\n" + parser.parseBody().getBody());
//        System.out.println(desc + " ends=====================");
        requestBuf.release();
        InvocationContextImpl.Factory.removeCurrentInstance();

        t1 += middle - begin;
        t2 += System.nanoTime() - middle;
    }

//    private static void doTest3(Service service, Method method, Struct struct, String json, String desc) throws TException {
//
//        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.createNewInstance();
//        invocationContext.codecProtocol(CodecProtocol.CompressedBinary);
//
//        invocationContext.serviceName(service.name);
//        invocationContext.versionName(service.meta.version);
//        invocationContext.methodName(method.name);
//        invocationContext.callerMid("JsonCaller");
//
//        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);
//
////        String hex = "000000b50201000000000a0b00010000002c636f6d2e746f6461792e6170692e676f6f64732e736572766963652e4f70656e476f6f6473536572766963650b0002000000156c697374536b7544657461696c4279536b754e6f730b000300000005312e302e300b00040000000a4a736f6e43616c6c6572080005c0a8c7ab0a0009b81cc7ab00007e940d00170b0b00000000000c00010f00010b000000010000000832303534333833390f0003080000000100000002000003";
//        String hex = "000000d4020101000496730b00010000002c636f6d2e746f6461792e6170692e676f6f64732e736572766963652e4f70656e476f6f6473536572766963650b0002000000156c697374536b7544657461696c4279536b754e6f730b000300000005312e302e300b0004000000252f6170692f6531626664373632333231653430396365653461633062366538343139363363080005ac1200020a0007ac1e00020000c30e08000879292c580a0009ac1e00020000c30d0d00170b0b00000000001c19f882800008323035343338333929f581800004000003";
////String hex = "000000d4020101000498670b00010000002c636f6d2e746f6461792e6170692e676f6f64732e736572766963652e4f70656e476f6f6473536572766963650b0002000000156c697374536b7544657461696c4279536b754e6f730b000300000005312e302e300b0004000000252f6170692f6531626664373632333231653430396365653461633062366538343139363363080005ac1200020a0007ac1e00020000c9e608000879292c580a0009ac1e00020000c9e50d00170b0b00000000001c19f881800008323035343534343429f581800004000003";
//        byte[] bytes = hexStr2bytes(hex);
//        requestBuf.setBytes(0, bytes);
//        requestBuf.writerIndex(bytes.length);
//
//        System.out.println("origJson:\n" + json);
//        System.out.println(dumpToStr(requestBuf));
//
//        JsonSerializer jsonDecoder = new JsonSerializer(service, method, "1.0.0", struct);
//        SoaMessageParser<String> parser = new SoaMessageParser<>(requestBuf, jsonDecoder);
//        parser.parseHeader();
//
//        System.out.println(parser.getHeader());
//        System.out.println("after enCode and decode:\n" + parser.parseBody().getBody());
//        System.out.println(desc + " ends=====================");
//        requestBuf.release();
//        InvocationContextImpl.Factory.removeCurrentInstance();
//    }

    private static Service getService(final String xmlFilePath) throws IOException {
        String xmlContent = IOUtils.toString(JsonSerializerTest.class.getResource(xmlFilePath), "UTF-8");
        return JAXB.unmarshal(new StringReader(xmlContent), Service.class);
    }

    private static String loadJson(final String jsonPath) throws IOException {
        return IOUtils.toString(JsonSerializerTest.class.getResource(jsonPath), "UTF-8");
    }
}
