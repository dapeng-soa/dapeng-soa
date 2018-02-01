package com.github.dapeng.tools;

import com.github.dapeng.tools.helpers.*;

/**
 * 命令行工具入口
 */
public class Main {
    private static final String RUNNING_INFO = "runningInfo";
    private static final String META_DATA = "metadata";
    private static final String REQUEST = "request";
    private static final String JSON = "json";
    private static final String XML = "xml";
    private static final String ROUT_INFO = "routeInfo";
    private static final String RUNNING_SERVICE = "runningServices";

    private static String help = "-----------------------------------------------------------------------\n" +
            " |commands: runningInfo | routInfo | metadata | request | json | xml  \n" +
            " | 1 .通过指定服务名，或服务名+版本号，获取对应的服务的容器ip和端口: \n" +
            " |    java -jar dapeng.jar runningInfo com.github.dapeng.soa.hello.service.HelloService\n" +
            " |    java -jar dapeng.jar runningInfo com.github.dapeng.soa.hello.service.HelloService 1.0.1\n" +
            " | 2. 通过服务名和版本号，获取元信息: \n" +
            " |    java -jar dapeng.jar metadata com.github.dapeng.soa.hello.service.HelloService 1.0.1\n" +
            " | 3. 通过json文件，请求对应服务，并打印结果: \n" +
            " |    java -jar dapeng.jar request request.json\n" +
            " | 4. 通过xml文件，请求对应服务，并打印结果: \n" +
            " |    java -jar dapeng.jar request request.xml\n" +
            " | 5. 通过系统参数，json文件，调用指定服务器的服务并打印结果: \n" +
            " |    java -Dsoa.service.ip=192.168.0.1 -Dsoa.service.port=9091 -jar dapeng.jar request request.json\n" +
            " | 6. 通过系统参数，xml文件，调用指定服务器的服务并打印结果: \n" +
            " |    java -Dsoa.service.ip=192.168.0.1 -Dsoa.service.port=9091 -jar dapeng.jar request request.xml\n" +
            " | 7. 通过服务名/版本号/方法名，获取请求json的示例: \n" +
            " |    java -jar dapeng.jar json com.github.dapeng.soa.hello.service.HelloService 1.0.0 sayHello\n" +
            " | 8. 通过服务名/版本号/方法名，获取请求xml的示例: \n" +
            " |    java -jar dapeng.jar xml com.github.dapeng.soa.hello.service.HelloService 1.0.0 sayHello\n" +
            " | 9. 获取当前zookeeper中的服务路由信息: \n" +
            " |    java -jar dapeng.jar routInfo\n" +
            " | 10.指定配置文件，设置路由信息: \n" +
            " |    java -jar dapeng.jar routInfo route.cfg \n" +
            " | 11. 获取指定的zookeeper上所有运行的服务列表"+
            "-----------------------------------------------------------------------";

    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            System.out.println(help);
            System.exit(0);
        }

        switch (args[0]) {
            case RUNNING_INFO:
                ZookeeperSerachHelper.getInfos(args);
                break;
            case META_DATA:
                MetaInfoHelper.getService(args);
                break;
            case REQUEST:
                RequestHelper.post(args);
                break;
            case JSON:
                RequestExampleHelper.getRequestJson(args);
                break;
            case XML:
                RequestExampleHelper.getRequestXml(args);
                break;
            case ROUT_INFO:
                RouteInfoHelper.routeInfo(args);
                break;
            case RUNNING_SERVICE:
                RunningServiceHelper.showRunningService();
                break;
            default:
                System.out.println(help);
        }

        System.exit(0);
    }
}
