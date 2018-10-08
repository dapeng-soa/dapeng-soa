package com.github.dapeng.json;

import com.github.dapeng.core.BeanSerializer;
import com.github.dapeng.core.InvocationContextImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaHeader;
import com.github.dapeng.core.enums.CodecProtocol;
import com.github.dapeng.core.helper.SoaHeaderHelper;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.core.metadata.Struct;
import com.github.dapeng.org.apache.thrift.TException;
import com.github.dapeng.util.SoaMessageBuilder;
import com.github.dapeng.util.SoaMessageParser;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.PreferHeapByteBufAllocator;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.dapeng.util.DumpUtil.dumpToStr;
import static com.github.dapeng.util.DumpUtil.hexStr2bytes;

/**
 * Unit test for simple App.
 */
public class JsonSerializerTest {
    static long t1 = 0;
    static long t2 = 0;

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


//        final String purchaseDescriptorXmlPath = "/com.today.api.purchase.service.PurchaseService.xml";
//        Service purchaseService = getService(purchaseDescriptorXmlPath);
//
//        Method createTransferOrder = purchaseService.methods.stream().filter(method -> method.name.equals("createTransferOrder")).collect(Collectors.toList()).get(0);
//        String json = loadJson("/createTransferOrder.json");
//
//        String desc = "createTransferOrderTest.json";

//        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
//        Service orderService = getService(orderDescriptorXmlPath);
//
//        Method orderServicePayNotify = orderService.methods.stream().filter(method -> method.name.equals("createAppointmentForAvailable")).collect(Collectors.toList()).get(0);
//        String payNotifyJson = loadJson("/orderService_createAppointmentForAvailable-complexStruct.json");
//
//        String desc = "complexStructTest";

//        OptimizedMetadata.OptimizedService optimizedService = new OptimizedMetadata.OptimizedService(purchaseService);
//
//        OptimizedMetadata.OptimizedStruct optimizedStruct = new OptimizedMetadata.OptimizedStruct(createTransferOrder.request);
//        for (int i = 0; i < 100000; i++) {
////                String resulta = result.equals("{}") ? "{\"status\":1}" : result.substring(0, result.lastIndexOf('}')) + ",\"status\":1}";
////            doTest2(optimizedService, orderServicePayNotify, optimizedStruct, payNotifyJson, desc);
//                doTest2(optimizedService, createTransferOrder, optimizedStruct, json, desc);
//        }
//
//        t1 = 0;
//        t2 = 0;
//
//        long cost = 0;
//        int round = 5;
//        for (int j = 0; j < round; j++) {
//            long t11 = System.nanoTime();
//            for (int i = 0; i < 100000; i++) {
////                String resulta = result.equals("{}") ? "{\"status\":1}" : result.substring(0, result.lastIndexOf('}')) + ",\"status\":1}";
////                doTest2(optimizedService, orderServicePayNotify, optimizedStruct, payNotifyJson, desc);
//                doTest2(optimizedService, createTransferOrder, optimizedStruct, json, desc);
//            }
//            long t22 = System.nanoTime() - t11;
//            System.out.println("cost:" + t22/1000000);
//            cost += t22;
////            Thread.sleep(200);
//        }
//        System.out.println("average:" + cost/round/1000000);
//        System.out.println("average:" + t1/round/1000000);
//        System.out.println("average:" + t2/round/1000000);

        try {
            final String purchaseDescriptorXmlPath = "/com.today.api.purchase.service.PurchaseService.xml";
            OptimizedMetadata.OptimizedService purchaseService = new OptimizedMetadata.OptimizedService(getService(purchaseDescriptorXmlPath));

            Method createTransferOrder = purchaseService.getMethodMap().get("createTransferOrder");
            String json = loadJson("/createTransferOrder.json");

            String desc = "createTransferOrderTest.json";

            OptimizedMetadata.OptimizedStruct struct = constructOptimizedStruct(purchaseService, createTransferOrder.request);
            while (true) {
                doTest2(purchaseService, createTransferOrder, struct, json, desc);

                Thread.sleep(200);
            }
//            queryExportReportTest();
//            createTransferOrderTest();
//            optionalBooleanTest();
//            simpleStructTest();
//            simpleMapTest();
//            createTransferOrderTest();
//            intArrayTest();
//            intMapTest();
//            enumTest();
//            simpleStructWithEnumTest();
//            simpleStructWithOptionTest();
//
//            complexStructTest();
//            complexStructTest1();
//            noTagStructTest();
//            memberRegisterByUnionIdAndOpenIdServiceTest();
//            concurrentTest();
        } catch (Exception e) {
            Thread.sleep(50);
            e.printStackTrace();
        } finally {
            System.exit(0);
        }

    }

    static AtomicInteger counter = new AtomicInteger(0);
    static AtomicInteger counter2 = new AtomicInteger(0);
    private static void concurrentTest() throws InterruptedException {
        long begin = System.currentTimeMillis();
        Executor ec = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10000; i++) {
            ec.execute(() -> {
                try {
                    listCategoryDetailBySkuNosTest();
                    counter.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            });
            ec.execute(() -> {
                try {
                    listSkuDetailBySkuNosTest();
                    counter2.incrementAndGet();
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            });
        }
        ((ExecutorService) ec).shutdown();
        ((ExecutorService) ec).awaitTermination(100, TimeUnit.HOURS);
        System.out.println("end:" + (System.currentTimeMillis() - begin));
        System.out.println("counter/counter1:" + counter + "/" + counter2);
    }

    private static void noTagStructTest() throws IOException, TException {
        final String purchaseDescriptorXmlPath = "/com.today.api.purchase.query.service.PurchaseQueryService.xml";
        OptimizedMetadata.OptimizedService purchaseService = new OptimizedMetadata.OptimizedService(getService(purchaseDescriptorXmlPath));

        Method listUnClearedOrder = purchaseService.getMethodMap().get("listUnClearedOrder");
        String json = loadJson("/listUnClearedOrder-resp.json");

        String desc = "listUnClearedOrder-resp";
        doTest2(purchaseService, listUnClearedOrder, constructOptimizedStruct(purchaseService, listUnClearedOrder.response), json, desc);

    }

    private static void queryExportReportTest() throws IOException, TException {
        final String financeReportDescriptorXmlPath = "/com.today.api.financereport.service.ExportReportService.xml";
        OptimizedMetadata.OptimizedService financeReportService = new OptimizedMetadata.OptimizedService(getService(financeReportDescriptorXmlPath));

        Method queryExportReport = financeReportService.getMethodMap().get("queryExportReport");
        String json = loadJson("/queryExportReport.json");

        String desc = "queryExportReportTest";
        doTest2(financeReportService, queryExportReport, constructOptimizedStruct(financeReportService, queryExportReport.request), json, desc);

    }

    private static void memberCouponQueryListServiceTest() throws IOException, TException {
        final String memberCouponQueryListServiceDescriptorXmlPath = "/com.today.api.memberAdmin.service.MemberAdminService.xml";
        OptimizedMetadata.OptimizedService memberCouponQueryListService = new OptimizedMetadata.OptimizedService(getService(memberCouponQueryListServiceDescriptorXmlPath));

        Method memberCouponQuery = memberCouponQueryListService.getMethodMap().get("memberCouponQueryListService");
        String json = loadJson("/memberCouponQueryListService.json");

        String desc = "memberCouponQueryListServiceTest";
//        doTest2(memberCouponQueryListService, memberCouponQuery, constructOptimizedStruct(memberCouponQueryListService, memberCouponQuery.request), json, desc);
        doTest3(memberCouponQueryListService, memberCouponQuery, constructOptimizedStruct(memberCouponQueryListService, memberCouponQuery.response), json, desc);

    }

    private static void createTransferOrderTest() throws IOException, TException {
        final String purchaseDescriptorXmlPath = "/com.today.api.purchase.service.PurchaseService.xml";
        OptimizedMetadata.OptimizedService purchaseService = new OptimizedMetadata.OptimizedService(getService(purchaseDescriptorXmlPath));

        Method createTransferOrder = purchaseService.getMethodMap().get("createTransferOrder");
        String json = loadJson("/createTransferOrder.json");

        String desc = "createTransferOrderTest.json";
        doTest2(purchaseService, createTransferOrder, constructOptimizedStruct(purchaseService, createTransferOrder.request), json, desc);

    }

    /**
     * 多余属性处理
     *
     * @throws IOException
     */
    private static void redundancyTest() throws IOException, TException {
        final String categoryDescriptorXmlPath = "/com.today.api.category.service.CategoryService.xml";
        OptimizedMetadata.OptimizedService categoryService = new OptimizedMetadata.OptimizedService(getService(categoryDescriptorXmlPath));

        Method createCategoryAttribute = categoryService.getMethodMap().get("createCategoryAttribute");
        String json = loadJson("/categoryService_createCategoryAttribute.json");

        String desc = "redundancyTest";
        doTest2(categoryService, createCategoryAttribute, constructOptimizedStruct(categoryService, createCategoryAttribute.request), json, desc);

    }

    private static void listCategoryDetailBySkuNosTest() throws IOException, TException {
        final String categoryDescriptorXmlPath = "/com.today.api.category.service.OpenCategoryService.xml";
        OptimizedMetadata.OptimizedService categoryService = new OptimizedMetadata.OptimizedService(getService(categoryDescriptorXmlPath));

        Method createCategoryAttribute = categoryService.getMethodMap().get("listCategoryDetailBySkuNos");
        String json = loadJson("/listCategoryDetailBySkuNos.json");

        String desc = "listCategoryDetailBySkuNosTest";
        doTest2(categoryService, createCategoryAttribute, constructOptimizedStruct(categoryService, createCategoryAttribute.request), json, desc);

    }

    private static OptimizedMetadata.OptimizedStruct constructOptimizedStruct(OptimizedMetadata.OptimizedService optimizedService, Struct struct) {
        return optimizedService.getOptimizedStructs().get(struct.namespace + "." + struct.name);
    }

    private static void listSkuDetailBySkuNosTest() throws IOException, TException {
        final String categoryDescriptorXmlPath = "/com.today.api.goods.service.OpenGoodsService.xml";
        OptimizedMetadata.OptimizedService categoryService = new OptimizedMetadata.OptimizedService(getService(categoryDescriptorXmlPath));

        Method createCategoryAttribute = categoryService.getMethodMap().get("listSkuDetailBySkuNos");
        String json = loadJson("/listSkuDetailBySkuNos.json");

        String desc = "listSkuDetailBySkuNosTest";
        doTest2(categoryService, createCategoryAttribute, constructOptimizedStruct(categoryService, createCategoryAttribute.request), json, desc);

    }


    private static void optionalBooleanTest() throws IOException, TException {
        final String supplierDescriptorXmlPath = "/com.today.api.supplier.service.SupplierService.xml";
        OptimizedMetadata.OptimizedService orderService = new OptimizedMetadata.OptimizedService(getService(supplierDescriptorXmlPath));

        Method createSupplierToGoods = orderService.getMethodMap().get("createSupplierToGoods");
        String json = loadJson("/supplierService_optionalBooleanStruct.json");

        String desc = "optionalBooleanTest";
        doTest2(orderService, createSupplierToGoods, constructOptimizedStruct(orderService, createSupplierToGoods.request), json, desc);

        json = loadJson("/supplierService_optionalBooleanStruct1.json");
        desc = "optionalBooleanTest-without-optional-field";
        doTest2(orderService, createSupplierToGoods, constructOptimizedStruct(orderService, createSupplierToGoods.request), json, desc);

    }

    private static void complexStructTest() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        OptimizedMetadata.OptimizedService orderService = new OptimizedMetadata.OptimizedService(getService(orderDescriptorXmlPath));

        Method orderServicePayNotify = orderService.getMethodMap().get("createAppointmentForAvailable");
        String payNotifyJson = loadJson("/orderService_createAppointmentForAvailable-complexStruct.json");

        String desc = "complexStructTest";
        doTest2(orderService, orderServicePayNotify, constructOptimizedStruct(orderService, orderServicePayNotify.request), payNotifyJson, desc);

    }

    private static void complexStructTest1() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        OptimizedMetadata.OptimizedService orderService = new OptimizedMetadata.OptimizedService(getService(orderDescriptorXmlPath));

        Method orderServicePayNotify = orderService.getMethodMap().get("createAppointmentForAvailable1");
        String payNotifyJson = loadJson("/complexStruct.json");

        String desc = "complexStructTest1";
        doTest2(orderService, orderServicePayNotify, constructOptimizedStruct(orderService, orderServicePayNotify.request), payNotifyJson, desc);

    }

    private static void simpleStructTest() throws TException, IOException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        OptimizedMetadata.OptimizedService orderService = new OptimizedMetadata.OptimizedService(getService(orderDescriptorXmlPath));

        Method orderServicePayNotify = orderService.getMethodMap().get("payNotify");
        String payNotifyJson = loadJson("/orderService_payNotify.json");

        String desc = "simpleStructTest";
        doTest2(orderService, orderServicePayNotify, constructOptimizedStruct(orderService, orderServicePayNotify.request), payNotifyJson, desc);
    }

    /**
     * Map<String,String>
     *
     * @throws IOException
     * @throws TException
     */
    private static void simpleMapTest() throws IOException, TException {
        final String orderDescriptorXmlPath = "/com.github.dapeng.json.demo.service.OrderService.xml";
        OptimizedMetadata.OptimizedService orderService = new OptimizedMetadata.OptimizedService(getService(orderDescriptorXmlPath));

        Method method = orderService.getMethodMap().get("payNotifyForAlipay");
        String json = loadJson("/orderService_payNotifyForAlipay-map.json");

        doTest2(orderService, method, constructOptimizedStruct(orderService, method.request), json, "simpleMapTest");
    }

    /**
     * Map<Integer, String>
     *
     * @throws IOException
     * @throws TException
     */
    private static void intMapTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        OptimizedMetadata.OptimizedService crmService = new OptimizedMetadata.OptimizedService(getService(crmDescriptorXmlPath));

        String json = loadJson("/crmService_listDoctorsNameById-map.json");

        Method method = crmService.getMethodMap().get("listDoctorsNameById");

        doTest2(crmService, method, constructOptimizedStruct(crmService, method.response), json, "intMapTest");
    }

    private static void intArrayTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        OptimizedMetadata.OptimizedService crmService = new OptimizedMetadata.OptimizedService(getService(crmDescriptorXmlPath));

        String json = loadJson("/crmService_listDoctorsNameById-list.json");

        Method method = crmService.getMethodMap().get("listDoctorsNameById");

        doTest2(crmService, method, constructOptimizedStruct(crmService, method.request), json, "intArrayTest");
    }

    private static void enumTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        OptimizedMetadata.OptimizedService crmService = new OptimizedMetadata.OptimizedService(getService(crmDescriptorXmlPath));

        String json = loadJson("/crmService_modifyDoctorType-enum.json");

        Method method = crmService.getMethodMap().get("modifyDoctorType");

        doTest2(crmService, method, constructOptimizedStruct(crmService, method.request), json, "enumTest");
    }

    private static void simpleStructWithEnumTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        OptimizedMetadata.OptimizedService crmService = new OptimizedMetadata.OptimizedService(getService(crmDescriptorXmlPath));

        String json = loadJson("/crmService_saveFocusDoctor-structWithEnum.json");

        Method method = crmService.getMethodMap().get("saveFocusDoctor");

        doTest2(crmService, method, constructOptimizedStruct(crmService, method.request), json, "simpleStructWithEnumTest");
    }

    private static void simpleStructWithOptionTest() throws IOException, TException {
        final String crmDescriptorXmlPath = "/crm.xml";

        OptimizedMetadata.OptimizedService crmService = new OptimizedMetadata.OptimizedService(getService(crmDescriptorXmlPath));

        String json = loadJson("/crmService_getPatient-option.json");

        Method method = crmService.getMethodMap().get("getPatient");

        doTest2(crmService, method, constructOptimizedStruct(crmService, method.request), json, "simpleStructWithOptionTest");
    }

    private static void memberRegisterByUnionIdAndOpenIdServiceTest() throws IOException, TException {
        final String memberDescriptorXmlPath = "/com.today.api.member.service.MemberService.xml";

        OptimizedMetadata.OptimizedService memberService = new OptimizedMetadata.OptimizedService(getService(memberDescriptorXmlPath));

        String json = loadJson("/memberRegisterByUnionIdAndOpenIdService.json");

        Method method = memberService.getMethodMap().get("memberRegisterByUnionIdAndOpenIdService");

        doTest2(memberService, method, constructOptimizedStruct(memberService, method.request), json, "memberRegisterByUnionIdAndOpenIdServiceTest");
    }

    private static void doTest2(OptimizedMetadata.OptimizedService optimizedServicee, Method method, OptimizedMetadata.OptimizedStruct optimizedStruct, String json, String desc) throws TException {
        long begin = System.nanoTime();
        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.createNewInstance();
        invocationContext.codecProtocol(CodecProtocol.CompressedBinary);

        invocationContext.serviceName(optimizedServicee.service.name);
        invocationContext.versionName(optimizedServicee.service.meta.version);
        invocationContext.methodName(method.name);
        invocationContext.callerMid("JsonCaller");

        JsonSerializer jsonSerializer = new JsonSerializer(optimizedServicee, method, "1.0.0", optimizedStruct);

        ByteBuf buf = buildRequestBuf(optimizedServicee.service.name, "1.0.0", method.name, 10, json, jsonSerializer);
//        System.out.println("origJson:\n" + json);
//
//
//        System.out.println(dumpToStr(buf));

        long middle = System.nanoTime();

        JsonSerializer jsonDecoder = new JsonSerializer(optimizedServicee, method, "1.0.0", optimizedStruct);

        SoaMessageParser<String> parser = new SoaMessageParser<>(buf, jsonDecoder);
        parser.parseHeader();
//        parser.getHeader();
        parser.parseBody();
//        System.out.println(parser.getHeader());
//        System.out.println("after enCode and decode:\n" + parser.getBody());
//        System.out.println(desc + " ends=====================" + "counters:" + counter + "/" + counter2);
        buf.release();
        InvocationContextImpl.Factory.removeCurrentInstance();

        t1 += middle - begin;
        t2 += System.nanoTime() - middle;
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    private static void doTest3(OptimizedMetadata.OptimizedService service, Method method, OptimizedMetadata.OptimizedStruct struct, String json, String desc) throws TException {

        InvocationContextImpl invocationContext = (InvocationContextImpl) InvocationContextImpl.Factory.createNewInstance();
        invocationContext.codecProtocol(CodecProtocol.CompressedBinary);

        invocationContext.serviceName(service.getService().name);
        invocationContext.versionName(service.getService().meta.version);
        invocationContext.methodName(method.name);
        invocationContext.callerMid("JsonCaller");

        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);

//        String hex = "000000b50201000000000a0b00010000002c636f6d2e746f6461792e6170692e676f6f64732e736572766963652e4f70656e476f6f6473536572766963650b0002000000156c697374536b7544657461696c4279536b754e6f730b000300000005312e302e300b00040000000a4a736f6e43616c6c6572080005c0a8c7ab0a0009b81cc7ab00007e940d00170b0b00000000000c00010f00010b000000010000000832303534333833390f0003080000000100000002000003";
        String hex = "00000975020101000000550b000100000034636f6d2e746f6461792e6170692e6d656d62657241646d696e2e736572766963652e4d656d62657241646d696e536572766963650b00020000001c6d656d626572436f75706f6e51756572794c697374536572766963650b000300000005312e302e300b000400000009436d6443616c6c6572080005c0a814c80a0007b56f14c8e62df7640a0009b56f14c8e62df6d90b000b00000004303030300b000c000000026f6b0a000dac190002173c8c0b08000ec0a80a7e080010000023800b001200000057636f6d2e746f6461792e6170692e6d656d62657241646d696e2e736572766963652e4d656d62657241646d696e536572766963653a6d656d626572436f75706f6e51756572794c697374536572766963653a312e302e300800150000009a0800160000009a0d00170b0b00000000000c001c1500151415320019ac16a0cd830318044576657216a8ab9d0b16a8ab9d0b1816e4b889e6988ee6b2bb35e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e150216f0b5f3d3955918143832313038353037353131303938363835343436180016d0b7c5b59559150216f0b5f3d3955915021800180b3133373531373737313031182433304236314139372d394639452d333433322d463935332d3434373642313232313732381680a60e1809e8b68ae7a780e5ba9716bce3f80e17000000000000144015080016a0cd830318044576657216aaab9d0b16aaab9d0b1823e585a8e59cbae6bba1313030e58583e7ab8be5878f3230e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e1500160018143832313038353130373831333233313830363934180016d0b7c5b59559150216d0b7c5b5955915021800180b3133373531373737313031182446413934443944362d433134382d323342422d363337392d36363841333830453242323416001800160017000000000000344015080016a0cd830318044576657216acab9d0b16acab9d0b1821e9b29ce9a39fe6bba13230e58583e7ab8be5878f35e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e15021680d4a7b09d5918143832313038353037353431303934393531313636180016d0b7c5b5955915021680d4a7b09d5915021800180b3133373531373737313031182443364433464432312d433639412d323033462d453338302d4134313033414342424335361680a60e1809e8b68ae7a780e5ba9716f09da31117000000000000144015080016a0cd830318044576657216aeab9d0b16aeab9d0b1821e9b29ce9a39fe6bba13230e58583e7ab8be5878f35e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e1500160018143832313038353130383131303336363530393639180016d0b7c5b59559150216d0b7c5b5955915021800180b3133373531373737313031182443364433464432312d433639412d323033462d453338302d41343130334143424243353616001800160017000000000000144015080016a0cd830318044576657216b0ab9d0b16b0ab9d0b1821e9b29ce9a39fe6bba13230e58583e7ab8be5878f35e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e1500160018143832313038353037353531343739303434333432180016d0b7c5b59559150216d0b7c5b5955915021800180b3133373531373737313031182443364433464432312d433639412d323033462d453338302d41343130334143424243353616001800160017000000000000144015080016a0cd830318044576657216b2ab9d0b16b2ab9d0b1821e9b29ce9a39fe6bba13230e58583e7ab8be5878f35e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e1500160018143832313038353037353631383738383130373533180016d0b7c5b59559150216d0b7c5b5955915021800180b3133373531373737313031182443364433464432312d433639412d323033462d453338302d41343130334143424243353616001800160017000000000000144015080016a0cd830318044576657216eab19d0b16eab19d0b181b546f646179e99c9ce6b787e6b78b38e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e1500160018143832313038353037353031373332373930353833180016d0b7c5b59559150216d0b7c5b5955915021800180b3133373531373737313031182439353639454436322d413642322d413734342d423631422d32453635453241364132363016001800160017000000000000204015080016a0cd830318044576657216ecb19d0b16ecb19d0b1816e4b889e6988ee6b2bb35e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e150216f0879dd6965918143832313038353130373631313132363939353536180016d0b7c5b59559150216f0879dd6965915021800180b3133373531373737313031182433304236314139372d394639452d333433322d463935332d3434373642313232313732381680a60e1809e8b68ae7a780e5ba9716b0ada10f17000000000000144015080016a0cd830318044576657216eeb19d0b16eeb19d0b1813e4bebfe5bd9336e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e150216b09bc3d6985918143832313038353037353231333732383635333232180016d0b7c5b59559150216b09bc3d6985915021800180b3133373531373737313031182445444530453133312d373234322d374646382d334143322d34323844333443413443333516c6a70e180ce5beaae7a4bce997a8e5ba9716f693ec0f17000000000000184015080016a0cd830318044576657216f0b19d0b16f0b19d0b1813e4bebfe5bd9336e58583e4bba3e98791e588b8168090c28f955916b0b085e5a75918026f6b16d0b7c5b595591500160e150216f0a69be9985918143832313038353037353331353538323437363531180016d0b7c5b59559150216f0a69be9985915021800180b3133373531373737313031182445444530453133312d373234322d374646382d334143322d34323844333443413443333516c6a70e180ce5beaae7a4bce997a8e5ba9716e8b6f30f17000000000000184015080016a0cd8303180b3133373531373737313031000003";
//String hex = "000000d4020101000498670b00010000002c636f6d2e746f6461792e6170692e676f6f64732e736572766963652e4f70656e476f6f6473536572766963650b0002000000156c697374536b7544657461696c4279536b754e6f730b000300000005312e302e300b0004000000252f6170692f6531626664373632333231653430396365653461633062366538343139363363080005ac1200020a0007ac1e00020000c9e608000879292c580a0009ac1e00020000c9e50d00170b0b00000000001c19f881800008323035343534343429f581800004000003";
        byte[] bytes = hexStr2bytes(hex);
        requestBuf.setBytes(0, bytes);
        requestBuf.writerIndex(bytes.length);

//        System.out.println("origJson:\n" + json);
        System.out.println(dumpToStr(requestBuf));

        JsonSerializer jsonDecoder = new JsonSerializer(service, method, "1.0.0", struct);
        SoaMessageParser<String> parser = new SoaMessageParser<>(requestBuf, jsonDecoder);
        parser.parseHeader();

        System.out.println(parser.getHeader());
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

    private static <REQ> ByteBuf buildRequestBuf(String service, String version, String method, int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        AbstractByteBufAllocator allocator =
                SoaSystemEnvProperties.SOA_POOLED_BYTEBUF ?
                        PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;
        final ByteBuf requestBuf = allocator.buffer(8192);

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        try {
            SoaHeader header = SoaHeaderHelper.buildHeader(service, version, method);

            ByteBuf buf = builder.buffer(requestBuf)
                    .header(header)
                    .body(request, requestSerializer)
                    .seqid(seqid)
                    .build();
            return buf;
        } catch (TException e) {
            e.printStackTrace();
        }

        return null;
    }
}
