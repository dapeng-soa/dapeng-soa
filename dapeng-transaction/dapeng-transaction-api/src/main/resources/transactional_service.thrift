include "transactional_domain.thrift"

namespace java com.github.dapeng.soa.transaction.api.service

service GlobalTransactionService {

   transactional_domain.TGlobalTransaction create(1:transactional_domain.TGlobalTransaction globalTransaction),

   void update(1:i32 globalTransactionId, 2:i32 currSequence, 3:transactional_domain.TGlobalTransactionsStatus status),

}

service GlobalTransactionProcessService {

    transactional_domain.TGlobalTransactionProcess create(1:transactional_domain.TGlobalTransactionProcess globalTransactionProcess),

    void update(1:i32 globalTransactionProcessId, 2:string responseJson, 3:transactional_domain.TGlobalTransactionProcessStatus status)

}