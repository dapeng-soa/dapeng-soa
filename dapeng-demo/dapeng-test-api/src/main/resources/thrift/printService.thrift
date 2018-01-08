include "info.thrift"
include "account_enums.thrift"

namespace java com.github.dapeng.soa.service

service PrintService{

    void print(),

    string printInfo(1:info.Info info),

    string printInfo2(1:string name)

    string printInfo3()
}