/*
package com.today.commons;

import com.github.dapeng.core.SoaException;
import com.today.soa.idgen.IDServiceClient;
import com.today.soa.idgen.domain.GenIDRequest;
import com.today.soa.idgen.service.IDService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

*/
/**
 * @author huyj
 * @Created 2019-02-19 16:23
 *//*

public class GenIdUtil {

    private static final Logger logger = LoggerFactory.getLogger(GenIdUtil.class);
    private static IDService idService = new IDServiceClient();
    public static String TASK_EVENT_ID = "task_event_id";

    */
/**
     * 获取主键id
     *
     * @param bizTag
     * @return
     *//*

    public static Long getId(String bizTag) {
        //System.setProperty("soa_zookeeper_host","123.206.103.113:2181")
        GenIDRequest genIDRequest = new GenIDRequest();
        genIDRequest.setBizTag(bizTag);
        genIDRequest.setStep(1);

        try {
            return idService.genId(genIDRequest);
        } catch (SoaException e) {
            logger.error(e.getMsg(), e);
        }
        return 0L;
    }
}
*/
