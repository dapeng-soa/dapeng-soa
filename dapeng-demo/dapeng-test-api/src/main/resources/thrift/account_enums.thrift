namespace java com.github.dapeng.soa.account.enums

/**
* 币种
**/
enum CurrencyType {

    /**
    * 人民币
    **/
    CNY,

    /**
    * 美元
    **/
    USD

}

/**
* 业务类型
**/
enum AccountJournalBusinessCategory {

    /**
    * 快塑账户
    **/
    KUAISU = 1,

    /**
    * 竞拍
    **/
    AUCTION = 2,

    /**
    * 一口价
    **/
    FIXEDPRICE = 3,

    /**
    * 电子盘
    **/
    ETRADE = 4

}

/**
* 账户类型
**/
enum AccountType {

    /**
    * 资金账户
    **/
    CAPITAL = 1,

    /**
    * 贷款账号
    **/
    CREDIT = 2,

    /**
    * 预付账户
    **/
    PREPAY = 3;

}




/**
* 账户流水交易类型
**/
enum AccountJournalTransType {

    /**
    * 收入
    **/
    INCOME = 1,

    /**
    * 支出
    **/
    EXPENDITURE = 2,

    /**
    * 冻结
    **/
    FREEZE = 3,

    /**
    * 解冻
    **/
    THAW = 4,

    /**
    * 提现
    **/
    WITHDRAWALS = 5,

    /**
    * 充值
    **/
    RECHARGE = 6

}

/**
* 冻结状态
**/
enum AccountJournalFreezeStatus {

    /**
    * 已解冻
    **/
    THAW = 1,

    /**
    * 冻结中
    **/
    FROZENIN = 2,

    /**
    * 部分冻结
    **/
    PARTIALFROZENIN = 3

}

/**
* 业务类型
**/
enum AccountJournalBusinessType {

    /**
    * 无业务类型
    **/
    NONE = 0,

    /**
    * 提现
    **/
    WITHDRAWALS = 1,

    /**
    * 充值
    **/
    RECHARGE = 2,

    /**
    * 订单货款
    **/
    ORDERSPAYMENT = 3,

    /**
    * 订单违约金
    **/
    ORDERSPENALTY = 4,

    /**
    * 收款转冻结保证金
    **/
    RECEIPT2FREEZE = 5

}

/**
* 申请类型
**/
enum TAccountApplyType {

    /**
    * 提现
    **/
    Withdrawals = 1,

    /**
    * 充值
    **/
    Recharge = 2

}

/**
* 申请状态
**/
enum TAccountApplyStatus {

    /**
    * 待处理
    **/
    Pending = 1,

    /**
    * 处理中
    **/
    Processing = 2,

    /**
    * 审批通过
    **/
    HasWithdrawals = 3,

    /**
    * 已制单
    **/
    HasBill = 4,

    /**
    * 成功
    **/
    Success = 5,

    /**
    * 失败
    **/
    Failed = 6

}
/**
*预付款账户回款申请 状态 0 未确认:(unconfirmed),1:已确认(confirmed),2:失败(failed)
**/
enum PrepayReceiptStatus{
/**
* 未确认
**/
UnConfirmed = 0,
/**
* 已确认
**/
Confirmed = 1,
/**
* 失败
**/
Failed = 2
}