package demo;

import com.github.dapeng.api.RemoteStoreService;
import com.github.dapeng.api.dto.StoreRequest;
import com.github.dapeng.api.dto.StoreResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Spencer Gibb
 */
@SpringBootApplication
@EnableDiscoveryClient
@RestController
@EnableFeignClients({"com.github.dapeng.api", "demo"})
public class HelloClientApplication {

    @Autowired
    HelloClient client;

    @RequestMapping("/")
    public String hello() {
        return client.hello();
    }

    @Autowired
    private RemoteStoreService storeService;

    @RequestMapping("/store")
    public StoreResponse hello(@RequestBody StoreRequest request) {
        return storeService.store(request);
    }

    public static void main(String[] args) {
        SpringApplication.run(HelloClientApplication.class, args);
    }

    @FeignClient("dapeng-sc-demo")
    interface HelloClient {
        @RequestMapping(value = "/", method = GET)
        String hello();
    }
}
