package demo.controller;


import com.github.dapeng.api.RemoteStoreService;
import com.github.dapeng.api.dto.StoreRequest;
import com.github.dapeng.api.dto.StoreResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
public class FeignController {

    @Autowired
    private RemoteStoreService storeService;

    @RequestMapping("/store")
    public StoreResponse hello(@RequestBody StoreRequest request) {
        return storeService.store(request);
    }
}
