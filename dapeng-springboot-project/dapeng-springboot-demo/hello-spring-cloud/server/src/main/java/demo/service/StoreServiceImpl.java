package demo.service;

import com.github.dapeng.api.RemoteStoreService;
import com.github.dapeng.api.dto.StoreRequest;
import com.github.dapeng.api.dto.StoreResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StoreServiceImpl implements RemoteStoreService {
    private Logger log = LoggerFactory.getLogger(getClass());

    private String machine = System.getProperty("machine");

    @Override
    public StoreResponse store(StoreRequest request) {
        log.info("StoreRequest: {}", request);

        StoreResponse response = new StoreResponse();
        response.setPageId("Page_" + request.getNumberId());
        response.setDetail("From " + machine + ",time: " + System.currentTimeMillis());

        return response;
    }
}
