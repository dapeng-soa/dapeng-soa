package com.github.dapeng.impl.plugins;

import com.github.dapeng.api.Container;
import com.github.dapeng.core.Plugin;
import com.github.dapeng.core.ProcessorKey;
import com.github.dapeng.core.definition.SoaServiceDefinition;
import com.github.dapeng.transaction.api.GlobalTransactionFactory;
import com.github.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.github.dapeng.transaction.api.service.GlobalTransactionService;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransactionContainer
 *
 * @author craneding
 * @date 16/4/11
 */
public class TransactionPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPlugin.class);

    private final Container container;

    public TransactionPlugin( Container container){
        this.container = container;
    }

    @Override
    public void start() {
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {
            System.out.println(" Start to initialize transaction plugin......");

            try {

                ProcessorKey transactionKey = new ProcessorKey("com.github.dapeng.transaction.api.service.GlobalTransactionService","1.0.0");
                SoaServiceDefinition<GlobalTransactionService> transactionServiceDef = (SoaServiceDefinition<GlobalTransactionService>)container.getServiceProcessors().get(transactionKey);

                ProcessorKey processKey = new ProcessorKey("com.github.dapeng.transaction.api.service.GlobalTransactionProcessService","1.0.0");
                SoaServiceDefinition<GlobalTransactionProcessService>processServicesDef = (SoaServiceDefinition<GlobalTransactionProcessService> )container.getServiceProcessors().get(processKey);

                if (transactionServiceDef!=null) {
                    GlobalTransactionFactory.setGlobalTransactionService(transactionServiceDef.iface);
                } else {
                    LOGGER.warn("----------- No GlobalTransactionService Found..-------");
                }
                if (processServicesDef!=null) {
                    GlobalTransactionFactory.setGlobalTransactionProcessService(processServicesDef.iface);
                } else {
                    LOGGER.warn("----------- No GlobalTransactionProcessService Found --------");
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {

    }

}
