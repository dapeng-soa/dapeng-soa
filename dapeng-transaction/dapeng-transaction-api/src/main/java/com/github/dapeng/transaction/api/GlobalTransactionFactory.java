package com.github.dapeng.transaction.api;

import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;

/**
 * Soa Transactional Manager
 *
 * @author craneding
 * @date 16/4/11
 */
public class GlobalTransactionFactory {

    private static GlobalTransactionService globalTransactionService;

    private static GlobalTransactionProcessService globalTransactionProcessService;

    public static GlobalTransactionService getGlobalTransactionService() {
        return globalTransactionService;
    }

    public static void setGlobalTransactionService(GlobalTransactionService globalTransactionService) {
        GlobalTransactionFactory.globalTransactionService = globalTransactionService;
    }

    public static GlobalTransactionProcessService getGlobalTransactionProcessService() {
        return globalTransactionProcessService;
    }

    public static void setGlobalTransactionProcessService(GlobalTransactionProcessService globalTransactionProcessService) {
        GlobalTransactionFactory.globalTransactionProcessService = globalTransactionProcessService;
    }

}
