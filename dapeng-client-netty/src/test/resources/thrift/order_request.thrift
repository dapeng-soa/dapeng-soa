namespace java com.github.dapeng.json.demo.domain

include "common_domain.thrift"
include "order_enum.thrift"
/**创建排班请求*/
struct BatchAppointmentRequest{
    /**排班医生*/
     1 : i32 doctorId,
     /**排班选择情况 map<appointmentTimeId,是否选中>*/
     3 : map<i32,bool> appointmentList,
}

/*创建订单请求包*/
struct CreateOrderRequest{
   /**
   * 医生 ID
   */
 1 :  i32 doctorId,
   /**
   * 客户 ID
   */
 2 :  i32 patientId,
   /**
   * 患者补充的症状描述
   */
 4 :optional  string patientInformationContent,
   /**
   * 预约信息 ID
   */
 5 :  i32 appointmentId,
   /**
    * 支付类型,1:支付宝(alipay);2:微信(wechat);
    */
 6:  order_enum.TOrderPayType payType,
 /**
 * 备注
 */
 7 : optional string remark,
  /**
  * 推荐医生 ID
  */
  8 : optional i32 recommendDoctorId,

  /**
  * 使用优惠券id
  */
  9 : optional i32 couponId,

}

/**
* 创建[可领取]的排班请求
**/
struct CreateAppointmentForAvailableRequest{
    /**
    * 排班医生
    **/
    1: i32 doctorId,

    /**
    * 排班日期，格式yyyy-MM-dd
    **/
    2: list<string> appointmentDate,
    3: common_domain.PageRequest pageRequest,
    4: set<CreateOrderRequest> requests,
    5: list<list<list<CreateOrderRequest>>> requestLists,
    6: map<string, string> strMap,
    7: map<i32, string> i32Map,
    8: map<string, i64> stringI64Map,
    9: map<i32, CreateOrderRequest> i32StructMap,
    10:map<i32,map<string, string>> recMap,
    11:map<string, map<i32, CreateOrderRequest>> recStructMap,
    12:map<string, list<CreateOrderRequest>> mapList,
    13:list<map<i32, CreateOrderRequest>> listMap,
}

struct CreateAppointmentForAvailableRequest1{
    1:list<map<i32, CreateOrderRequest>> listMap,
}

/*查询排班列表请求条件*/
struct ListAppointmentsRequest{

 1 : common_domain.PageRequest pageRequest,

  /*请求来源*/
 2 : order_enum.QueryAppointmentFromEnum queryAppointmentFrom,
 /*医生id*/
 3 : optional i32 doctorId,
 /*医生姓名*/
 4 : optional string doctorName,

 /*医生预约开始时间*/
 5 : optional i64 appointmentBeginTime,

 /*医生预约开始时间*/
 6 : optional i64 appointmentEndTime,
 /*医生id*/
 7 : optional list<i32> idList,

}




/*创建线下订单请求包*/
struct CreateOrderForOffLineRequest{
   /**
   * 医生 ID
   */
 1 :  i32 doctorId,
   /**
   * 线下患者 ID, 新增患者，ID为0
   */
 2 : i32 offlinePatientId,
   /**
   * 线下患者 手机号码
    **/
 3 : string offlinePatientPhone
    /**
    * 线下患者 姓名
     **/
 4 : string offlinePatientName
     /**
     * 线下患者 性别
      **/
 5 : order_enum.Gender offlinePatientGender
    /**
    * 线下患者 年龄
    **/
 6 : i32 offlinePatientAge
   /**
   * 患者补充的症状描述
   */
 7 : string patientInformationContent,
   /**
   * 预约信息 ID
   */
 8 :  i32 appointmentId,
   /**
   * 备注
   */
 9 : optional string remark,
   /**
   * 订单状态,仅限 待支付和待诊断[已付款]
   **/
 10: optional order_enum.TOrderStatus status,
   /**
   * 治疗方式
   **/
  11: order_enum.TOrderTherapyMode therapyMode,

}

/*编辑线下订单请求包*/
struct ModifyOrderForOfflineRequest{
   /**
   * 订单ID
    **/
 1 : i32 orderId
    /**
    * 线下患者 姓名
     **/
 2 : string offlinePatientName
     /**
     * 线下患者 性别
      **/
 3 : order_enum.Gender offlinePatientGender
    /**
    * 线下患者 年龄
    **/
 4 : i32 offlinePatientAge
   /**
   * 患者补充的症状描述
   */
 5 : string patientInformationContent,
   /**
   * 备注
   */
 6 : optional string remark,
   /**
   * 治疗方式
   **/
 7: order_enum.TOrderTherapyMode therapyMode,
   /**
   * 是否首诊
   **/
 8: bool firstTherapy,

}

/*创建线下患者请求包*/
struct CreateOfflinePatientRequest{
   /**
   * 姓名
   */
 1 : string name,
   /**
   * 手机号码
   */
 2 : string phone,
   /**
   * 性别
   */
 3 : order_enum.Gender gender,
   /**
    * 年龄
    */
 4 : i32 age,

}

/*获取线下患者请求包*/
struct GetOfflinePatientRequest{
   /**
   * ID,二选一
   */
 1 : optional i32 id,
   /**
   * 手机号码,二选一
   */
 2 : optional string phone,

}

/*查询医生有效的图文包月订单（预约单）请求包*/
struct ListTextPackOrderForDoctorRequest{
 1: optional common_domain.PageRequest pageRequest,
 2: i32 doctorId
}

/*查询订单（预约单）请求包*/
struct ListOrderRequest{
 1: optional common_domain.PageRequest pageRequest,

 /**匹配状态（多选），如果空，则表示全部状态*/
 2: list<order_enum.TOrderStatus> statuses,

 /**患者ID*/
 3: optional i32 patientId,

 /**医生ID*/
 4: optional i32 doctorId,

 /**医生ID集合*/
 5:  list<i32> doctorIdList,

 /**开始时间*/
 6: optional i64 beginTime,

 /**结束时间*/
 7: optional i64 endTime,

 /**是否已经创建诊疗报告*/
 8: optional bool exitDiagnose,
 /**支付方式*/
 9: optional order_enum.TOrderPayType payType,
 /**预约方式*/
 10: optional order_enum.AppointmentType appointmentType,
}

/**
*  订单支付成功回调请求
*/
struct PayNotifyRequest{
    /*订单唯一ID*/
    1: string orderNum,
    /*支付平台的订单ID*/
    2: string transactionId,
    /*支付方式*/
    3: order_enum.TOrderPayType payType,
}

/**
*   创建诊断报告
*/
struct CreateDiagnoseRequest{
    /*订单ID*/
    1: i32 orderId,
    /*内容*/
    2: string content,
    /**
    * 推荐的医生id
    **/
    3: list<i32> recDoctorIds,
    /**
    * 推荐的量表类型
    **/
    4: list<string> recTitleCategories,

}

/**
*   查询诊断报告
**/
struct ListDiagnoseRequest{
    /**
    * 医生ID
    **/
    1: optional i32 doctorId,
    /**
    * 患者ID
    **/
    2: optional i32 patientId,
    /**分页*/
    3:optional common_domain.PageRequest pageRequest
}

/**
*   修改订单状态请求
*/
struct ModifyOrderStatusRequest{
    /*订单ID*/
    1: i32 orderId,

    /*修改后状态*/
    2:order_enum.TOrderStatus orderStatus,
    /**备注*/
    3:optional string remark
}

/**查询排班医生请求*/
struct ListAppointmentDoctorsRequest{
    /*分页*/
    1:optional common_domain.PageRequest pageRequest,

    /**医生标签，空表示匹配全部医生,必须登录用户才能使用该查询条件*/
    2:list<string> labels,
    /**
    * 排班类型
    **/
    3: optional order_enum.AppointmentType appointmentType,
}

/**查询排班医生请求*/
struct ListAppointmentForDoctorNameRequest{
    /*分页*/
    1: common_domain.PageRequest pageRequest,

    /**医生姓名*/
    2: string doctorName,
}

struct AppointmentDoctorsByDateRequest{
    /*日期：格式yyyy-MM-dd */
    1: string date,

    /*分页*/
    2:optional common_domain.PageRequest pageRequest
}
struct ListAppointmentPatientRequest{
 /**治疗状态 0：待诊断，1：已诊疗,2:已过期*/
  1:optional i32 hasTreated

  /**分页*/
  2:optional common_domain.PageRequest pageRequest,

  /**是否只查询最近记录*/
  3:optional bool  queryLatest,
}

struct ListAppointmentCalendarRequest{
   /**医生id*/
   1:optional i32 doctorId,
  /**
   * 预约类型
   **/
   2: order_enum.AppointmentType appointmentType,
}

/*查询医生排班(医生已排班的时间段)请求*/
struct ListAppointmentsByDoctorIdRequest{
/**医生id*/
1: i32 doctorId,
/**查询日期 格式yyyy-MM-dd*/
2: string date,
/**
* 预约类型
**/
3: optional order_enum.AppointmentType appointmentType,
}

/*查询医生排班(包括待排班)请求*/
struct ListAppointmentTimeForDoctorRequest{
    /**医生id*/
    1: i32 doctorId,
    /**查询日期 格式yyyy-MM-dd*/
    2: optional string date,
    3: optional string beginTime,
    4: optional string endTime,
    /**
    * 预约类型
    **/
    5: optional order_enum.AppointmentType appointmentType,
    /**
    * 时间段
    **/
    6: optional order_enum.TimeInterval timeInterval,
}

struct DiagnoseFinishRequest{
    /**订单id*/
    1:i32 orderId,
    /**备注*/
    2: optional string remark,
}

/**重做异常单请求包*/
struct RedoExceptionOrderRequest{
    /**原订单id*/
    1:i32 oldOrderId,
     /**新预约的排班id*/
    2:optional i32 newAppointmentId,
     /**新排班时间id，与newAppointmentId二选一，如果选择未排班时间段时，补全医生排班*/
    3:optional i32 newAppointmentTimeId,
    /**备注*/
    4:optional string remark,
}
struct PatientHomePageInfoRequest{
    /**患者ID*/
    1:optional i32 patientId,
}
struct PatientHomePageSliderNoticeRequest{
      /**
       * 返回记录数
       **/
       1: optional i32 limit,
}


/**
*
**/
struct SaveRecommendsRequest {
    /**
    * 患者id
    **/
    1: i32 patientId,
    /**
    * 订单id
    **/
    2: i32 orderId,
    /**
    * 推荐类型
    **/
    3: order_enum.RecommendTypeEnum recommendType,
    /**
    * 推荐的医生id
    **/
    4: optional list<i32> recDoctorIds,
    /**
    * 推荐的量表类型
    **/
    5: optional list<string> recTitleCategory,
    /**
    * 推荐人(医生)id
    **/
    6: i32 recommendBy,
}
struct ListRecommendsRequest {
    /**
    * 分页请求
    **/
    1: optional common_domain.PageRequest pageRequest,
    /**
    * 患者id
    **/
    2: i32 patientId,
    /**
    * 订单id
    **/
    3: i32 orderId,
    /**
    * 推荐类型
    **/
    4: optional order_enum.RecommendTypeEnum recommendType,
}
struct ListPriceRuleRequest {
    /*分页*/
    1: optional common_domain.PageRequest pageRequest,
    /**医院id*/
    2: optional i32 hospitalId,
    /**医生级别*/
    3: optional i32 level,
     /**
    * 排班类型
    **/
    4: optional order_enum.AppointmentType appointmentType,
}

/*查询医生套餐请求包*/
struct GetPackageInfoRequest{
    /*医生id*/
    1: i32 doctorId,
    /**
    * 套餐类型类型
    **/
    2: order_enum.AppointmentType appointmentType,
}

/*购买套餐请求包*/
struct BuyPackageRequest{
    /*医生id*/
    1: i32 doctorId,
    /**
    * 套餐类型类型
    **/
    2: order_enum.AppointmentType appointmentType,

    /**
     * 支付方式
     */
    3:order_enum.TOrderPayType payType,

    /**
     * 备注
     */
     4 : optional string remark,
}

/*判断是否可以聊天对话请求包*/
struct JudgeChatAbleRequest{
   /** 医生id*/
   1: i32 doctorId,
   /** 患者id*/
   2 : i32 patientId,
   /*订单id， 如果传了订单ID则只判断该订单情况*/
   3 : optional i32 orderId,
}

/**发放优惠券请求*/
struct GiveCouponRequest{
    /**优惠券类型*/
     1 : order_enum.TCouponType type,
     /**优惠券面值*/
     2:double amount,
     /**发放时间*/
     3:i64 publishAt,
     /**过期时间*/
     4:i64 expiredAt,
      /**客户id*/
     5:i32 patientId
}

struct ListAvaliableCouponsRequest{
       /**客户id*/
       1:i32 patientId,
        /**
       * 使用优惠券的订单类型
       **/
       2:optional order_enum.AppointmentType appointmentType,
}

/*购买陪护套餐请求包*/
struct CreateAccompanyOrderRequest{
    /**
    * 套餐类型类型
    **/
    1: order_enum.AccompanyPackEnum accompanyPackEnum,

    /**
     * 支付方式
     */
    2:order_enum.TOrderPayType payType,

    /**
     * 备注
     */
    3 : optional string remark,
}

/* 体验码列表 */
struct ListExperienceCouponRequest{
    /*分页*/
    1: optional common_domain.PageRequest pageRequest,
    /**
    * status
    **/
    2: optional order_enum.TCouponStatus status,

}