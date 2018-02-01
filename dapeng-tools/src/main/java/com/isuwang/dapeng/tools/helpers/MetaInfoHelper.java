package com.github.dapeng.tools.helpers;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.remoting.fake.metadata.MetadataClient;

/**
 * @author Eric on  2016/2/15.
 */
public class MetaInfoHelper {

    public static String getService(String... args) {
        if (args.length != 3) {
            System.out.println("example: java -jar dapeng.jar metadata com.github.dapeng.soa.hello.service.HelloService 1.0.1");
            System.exit(0);
        }
        String serviceName = args[1];
        String versionName = args[2];

        String metadata = "";
        try {
            System.out.println("Getting metadata ...");
            metadata = new MetadataClient(serviceName, versionName).getServiceMetadata();
            System.out.println("------------------------------------------------------");
            System.out.println(metadata);
        } catch (SoaException e) {
            e.printStackTrace();
        }
        return metadata;
    }
}
