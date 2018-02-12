namespace java com.isuwang.soa.settle.domain

struct Settle {

 1: i32 id,
 2: i32 orderId,
 3: double cash_debit,
 4: double cash_credit,
 5: optional string remark

}