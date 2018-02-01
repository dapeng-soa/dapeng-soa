namespace java com.github.dapeng.soa.service

include "info_domain.thrift"

service PrintService{

    void print(),

    string printInfo(1:info_domain.Info info),

    string printInfo2(1:string name)

    string printInfo3()
}