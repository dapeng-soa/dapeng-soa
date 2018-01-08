namespace java com.github.dapeng.soa.transaction.api.domain

enum TGlobalTransactionsStatus {

    New = 1,

    Success = 2,

    Fail = 3,

    HasRollback = 4,

    PartiallyRollback = 5,

    Suspend = 99

}

enum TGlobalTransactionProcessStatus {

    New = 1,

    Success = 2,

    Fail = 3,

    Unknown = 4,

    HasRollback = 5

}

enum TGlobalTransactionProcessExpectedStatus {

    Success = 1,

    HasRollback = 2

}

struct TGlobalTransaction {

    1:i32 id,

    2:TGlobalTransactionsStatus status,

    3:i32 currSequence,

    /**
    * @datatype(name="date")
    **/
    4:i64 createdAt,

    /**
    * @datatype(name="date")
    **/
    5:i64 updatedAt,

    6:i32 createdBy,

    7:i32 updatedBy

}

struct TGlobalTransactionProcess {

    1:i32 id,

    2:i32 transactionId,

    3:i32 transactionSequence,

    4:TGlobalTransactionProcessStatus status,

    5:TGlobalTransactionProcessExpectedStatus expectedStatus,

    6:string serviceName,

    7:string versionName,

    8:string methodName,

    9:string rollbackMethodName,

    10:string requestJson,

    11:string responseJson,

    /**
    * @datatype(name="date")
    **/
    12:i32 redoTimes,

     /**
     * @datatype(name="date")
     **/
    13:i64 nextRedoTime,

    /**
    * @datatype(name="date")
    **/
    14:i64 createdAt,

    /**
    * @datatype(name="date")
    **/
    15:i64 updatedAt,

    16:i32 createdBy,

    17:i32 updatedBy

}