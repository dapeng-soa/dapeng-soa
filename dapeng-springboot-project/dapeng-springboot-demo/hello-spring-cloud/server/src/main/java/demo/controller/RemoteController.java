package demo.controller;


import com.github.dapeng.api.RemoteStoreService;
import com.github.dapeng.api.dto.StoreRequest;
import com.github.dapeng.api.dto.StoreResponse;
import demo.service.StoreServiceImpl;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RemoteController {

    private final RemoteStoreService storeService;

    public RemoteController(StoreServiceImpl storeServiceImpl) {
        this.storeService = storeServiceImpl;
    }

    @RequestMapping("/store")
    public StoreResponse hello(@RequestBody StoreRequest request) {
        return storeService.store(request);
    }


}
