namespace java com.github.dapeng.json.demo.domain
include "order_domain.thrift"
include "common_domain.thrift"
include "order_enum.thrift"

/**患者信息*/
struct PatientInfo{
    /**
    *患者id
    */
    1: i32 id,
    /**患者名字*/
    2:optional string name,
    /**患者电话*/
    3:optional string phone,
    /**
    * 患者头像
    */
    4: optional string headImage,
    /**
    *  患者身份证
    **/
    5: optional string idcard,
    /**
    * 患者性别
    **/
    6: optional order_enum.Gender gender,
    /**
    * 年龄
    **/
    7: optional  i32 age,

}

/**医生信息*/
struct DoctorInfo{
    /**
    *医生id
    */
    1: i32 id,
    /**医生名字*/
    2:optional string name,
    /**医生电话*/
    3:optional string phone,
    /**
    * 医生头像
    */
    4: optional string headImage,
     /**
    * 医生职称
    **/
    5: optional string jobTitle,
    /**
    * 医生级别
    **/
    6: i32 level,

    /**特长标签*/
    7:optional string  labels,
    /**
    * 从业经历
    **/
    8: optional string jobExperience,

    /**
    * 所属医院名称
    **/
    9:optional string hospitalName,

    /**
    所属部门名称
    **/
    10:optional string departmentName,
    /**
    客服医生默认回复消息1
    **/
    11:optional string defaultReplyOne,
    /**
    客服医生默认回复消息2
    **/
    12:optional string defaultReplyTwo,
    /**
    客服医生默认回复消息3
    **/
    13:optional string defaultReplyThree,
}

/**医生排班信息项*/
struct AppointmentItemResponse{
     /**
       * 排班ID
       */
     1 :  i32 id,
       /**
       * 医生 ID
       */
     2 :  i32 doctorId,
       /**
       * 医生 姓名
       */
     3 :  string doctorName,
        /**
        * 医生等级
        */
     4 :  i32 level,
       /**
       *  医生可预约开始时间
       */
     5 :  i64 appointmentBeginTime,
       /**
       *  医生可预约开始时间
       */
     6 :  i64 appointmentEndTime,
       /**
       * 挂号费用
       */
     7 :  double registrationAmount,
      /**
        * 挂号费用
        */
     8 :  double discountRegistrationAmount,
       /**
       * 预约状态,1:可领取[医生可以排班的](available);2:可预约(workable);3:已预约(reserved);4:已过期[当天没人预约就置为过期](refund)
       */
     9 :  i32 status,
       /**
       * 创建时间
       */
     10 : i64 createdAt,
       /**
       *
       */
     11 : i32 createdBy,
       /**
       * 更新时间
       */
     12: i64 updatedAt,
       /**
       *
       */
     13 : i32 updatedBy,
       /**
       *
       */
     14 : string remark,
       /**
       *排班时间表的ID
       */
     15 : i32 appointmentTimeId,

}
/*医生排班列表响应包*/
struct ListAppointmentsResponse{
    /**
    *  预约单列表
    **/
    1: list<AppointmentItemResponse> appointments,
    /**
    *  分页查询响应
    **/
    2: common_domain.PageResponse pageResponse,
}

struct LastTreatDoctorInfo{
     /**
     *  doctorId
     **/
     1: i32 doctorId,
     /**
     *  医生名字
     **/
     2: string doctorName,

}
/*最后诊断医生*/
struct GetLastTreatDoctorResponse{
    1: optional LastTreatDoctorInfo doctorInfo
}

struct OrderItem{
    /**
    * 订单 ID
    */
    1 :  i32 id,
    /**
    * 医生 ID
    */
    2 :  i32 doctorId,
    /**
    * 患者 ID
    */
    3 :  i32 patientId,
    /**
    * 患者补充的症状描述
    */
    4 : optional string patientInformationContent,
    /**
    * 诊断信息ID
    */
    5 : optional i32 diagnoseId,
    /**
    * 预约信息 ID
    */
    6 :  i32 appointmentId,
    /**
    * 结算状态,1:新建[已挂号,未缴纳就诊费用](init);2:待诊断[已缴纳就诊费用](waiting_diagnosis);3:已过期[挂号后没在设定时间缴纳就诊费用](expired);4:已诊断[正常诊断完毕](confirmed_diagnosis);5:已退款[异常情况导致退诊断费用](refund)
    */
    7 :  order_enum.TOrderStatus status,
    /**
    * 治疗费
    */
    8 :  double therapyAmount,
    /**
    * 折扣后治疗费
    */
    9 :  double discountTherapyAmount,
    /**
    * 缴费过期时间
    */
    10 :  i64 registrationExpireTime,
    /**
    *  预约诊断开始时间
    */
    11 :  i64 therapyBeginTime,
    /**
    *  预约诊断结束时间
    */
    12 :  i64 therapyEndTime,
    /**
    * 创建时间
    */
    13 :  i64 createdAt,
    /**
    *
    */
    14 :  i32 createdBy,
    /**
    * 更新时间
    */
    15 :  i64 updatedAt,
    /**
    *
    */
    16 :  i32 updatedBy,
    /**
    *
    */
    17 : optional string remark,
    /**
    * 支付平台的唯一订单号
    */
    18 : optional string transactionId,
    /**
    * 预约订单的唯一订单号
    */
    19 :  string orderNum,
    /**
    * 支付类型,1:支付宝(alipay);2:微信(wechat);
    */
    20 :  order_enum.TOrderPayType payType,
    /**
    *  付款超时剩余秒数
    **/
    21: optional i64 expireRemainSeconds,
    /**
    *  预约类型
    */
    22 : order_enum.AppointmentType appointmentType,
    /**
    *  医生信息
    */
    24 : DoctorInfo doctorInfo,
    /**
    *(已预约)患者信息
    */
    25: PatientInfo patientInfo,
    /**
    * 父订单id;
    */
    26:optional i32 parentId,
    /**
    * 线下患者id;
    */
    27:optional i32 offlinePatientId,

    /**
    *诊疗方式
    */
    28:order_enum.TOrderTherapyMode therapyMode,
    /**
    *诊疗方式
    */
    29: optional string uuid
    /**
    * 医生是否已查看患者新单
    */
    30: optional bool read

    /**
    * 知心陪护套餐类型、天数
    **/
    31: optional order_enum.AccompanyPackEnum accompanyPackDays

  /**
    * 知心陪护套餐类型、天数
    **/
    32: double couponAmount,
}

/**
* 订单列表响应
**/
struct ListTextPackOrderForDoctorResponse{
    /**
    *  订单列表
    **/
    1: list<OrderItem> orderItems,
    /**
    *  分页查询响应
    **/
    2: optional common_domain.PageResponse pageResponse,
}
/**
* 订单列表响应
**/
struct ListOrderResponse{
    /**
    *  订单列表
    **/
    1: list<OrderItem> orders,
    /**
    *  分页查询响应
    **/
    2: common_domain.PageResponse pageResponse,
    /**
    * 合计线上有效预约金额
    **/
    3: double sumAmount,
    /**
    * 合计线下有效预约金额
    **/
    4: double sumOffLineAmount,
}

/*预约就诊计划(排队就诊)*/
struct TreatPlanItem{
    /**
     *  orderId
     **/
     1: i32 orderId,
     /**
     *  doctorId
     **/
     2: i32 doctorId,
     /**
     *  医生名字
     **/
     3: string doctorName,

      /**
      *  治疗开始时间
      **/
      4: i64 therapyBeginTime,
      /**
      *  治疗时间
      **/
      5: i64 therapyEndTime,
      /**
      *  剩余秒数
      **/
      6: optional i64 remainSeconds,
      /**
      *  剩余秒数
      **/
      7: order_enum.TOrderStatus status,
}

/*预约就诊计划(排队就诊)*/
struct ListTreatPlansResponse{
    /*今日就诊信息*/
    1:list<TreatPlanItem> todyPlans,

    /*以后就诊预约*/
    2:list<TreatPlanItem> futurePlans,
}

/*诊断报告*/
struct DiagnoseItem{
    /**
    * id
    **/
    1: i32 id,
    /**
    * doctorId
    **/
    2: i32 doctorId,
    /**
    * 医生姓名
    **/
    3: string doctorName,
    /**
    * 医生头像
    **/
    4: string doctorHeadImage,
    /**
    *  patientId
    **/
    5: i32 patientId,
    /**
    *  病人姓名
    **/
    6: string patientName,
    /**
    *  患者头像
    **/
    7: string patientHeadImage
    /**
    *  诊断内容
    **/
    8: string content,
    /**
    * 时间
    **/
    9: i64 createdAt,
    /*是否已阅读*/
    10: bool read
}

/*诊断报告列表*/
struct ListDiagnoseResponse{
    /**
    * 分页
    **/
    1: common_domain.PageResponse pageResponse,
    /**
    * list
    **/
    2: list<DiagnoseItem> diagnoses,
}

/**查询可预约医生*/
struct  AppointmentDoctor{
/**
    * doctorId
    **/
    1: i32 doctorId,
    /**
     * 医生名字
     **/
    2: string doctorName,
     /**
    * 头像路径
    **/
    3: optional string headImage,

    /**医生特长特长*/
    4: string forte,
    /**
    * 医生级别
    **/
    5: i32 level,

    /**可预约周期*/
    6: optional order_enum.AppointmentPeriod period,
    /**
    * 医生职称
    **/
    7: optional string jobTitle,
    /**
    * 从业经历
    **/
    8: optional string jobExperience,
    /**
    * 治疗费
    */
    9 : double therapyAmount,
    /**
    * 折扣后治疗费
    */
    10 : double discountTherapyAmount,
     /**
    * 优惠描述
    **/
    11: optional string discountDesc,

    /**
    * 所属医院名称
    **/
    12: string hospitalName,

    /**
    所属部门名称
    **/
    13: string departmentName,

    /**
    * 排班类型
    **/
    14: order_enum.AppointmentType appointmentType,

    /**
    * 医生简介
    **/
    15: optional string info,
}


/**查询可预约医生*/
struct  AppointmentDoctorsResponse{
 /**
    * 分页
    **/
    1 : common_domain.PageResponse pageResponse,

    2 : list<AppointmentDoctor> doctors

}

struct AppointmentDoctorByDate{
    /**
    * doctorId
    **/
    1: i32 doctorId,
      /**
        * doctor
        **/
    2: string doctorName,
     /**
    * 头像路径
    **/
    3: optional string headImage,

    /**医生特长特长*/
    4: string forte,

    /**
    * 医生级别
    **/
    5: i32 level,

     /**剩余号源*/
    6: i32 remains,
    /**
    * 医生职称
    **/
    7: optional string jobTitle,
    /**
    * 从业经历
    **/
    8: optional string jobExperience,
}
/**按天查询查询可预约医生*/
struct AppointmentDoctorsByDateResponse{
    /**
    * 分页
    **/
    1 : common_domain.PageResponse pageResponse,

    2 : list<AppointmentDoctorByDate> doctors,
    /**可以预约日期 yyyy-MM-dd*/
    3 : list<string> calendar
}
/**今日值班医生*/
struct ListDoctorOnDutyResponse{
/**
    * 分页
    **/
    1 : common_domain.PageResponse pageResponse,

    2 : list<AppointmentDoctorByDate> doctors,
}

/**已预约的患者*/
struct AppointmentPatient{
  /**
    * 患者id
    **/
    1: i32 patientId,
    /**
    * doctorId
    **/
    2: string patientName,
    /**
    * 患者头像路径
    **/
    3:optional string patientHeadImage,
    /**
    * 患者头像路径
    **/
    4:optional string patientInformationContent,
    /**
    *  预约诊断开始时间
    */
    5 :  i64 therapyBeginTime,
    /**
    *  预约诊断结束时间
    */
    6 :  i64 therapyEndTime,
    /**
    *  剩余时间毫秒数
    */
    7 :  i64 remainTime,

    /**
    *  预约类型
    */
    8 : optional order_enum.AppointmentType appointmentType,
     /**
    *  预约订单
    */
    9: i32 orderId,
     /**
      *  订单状态
    */
    10: order_enum.TOrderStatus status,
     /**
      * 诊断报告id
    */
    11: optional i32 diagnoseId,
    /**
      * 允许提前诊疗时间（分）
    */
    12: i32 advanceMini,

    /**
    * 电话
    **/
    13: string phone,

     /**
    * 诊疗方式
    **/
    14: order_enum.TOrderTherapyMode therapyMode,

     /**
    * 支付方式
    **/
    15: order_enum.TOrderPayType payType,

}
/*医生查询已预约的患者*/
struct ListAppointmentPatientResponse{
   /**
      * 分页
      **/
      1 : common_domain.PageResponse pageResponse,
      /**
      * 患者列表
      **/
      2 : list<AppointmentPatient> patients
}

struct AppointmentItem{
 /**
   * ID
   */
 1 :  i32 id,
   /**
   * 医生 ID
   */
 2 :  i32 doctorId,
    /**
    * 医生等级
    */
 3 :  i32 level,
   /**
   *  医生可预约开始时间
   */
 4 :  i64 appointmentBeginTime,
   /**
   *  医生可预约开始时间
   */
 5 :  i64 appointmentEndTime,
   /**
   * 挂号费用
   */
 6 :  double registrationAmount,
  /**
    * 患者补充的症状描述
    */
 7 :  double discountRegistrationAmount,
   /**
     * 费用折扣（几折，如8.8则为 8.8折）
     */
  8 :  double discount,
   /**
   * 预约状态,1:可领取[医生可以排班的](available);2:可预约(workable);3:已预约(reserved);4:已过期[当天没人预约就置为过期](refund)
   */
  9 :  order_enum.TAppointmentStatus status,
   /**
     * 是否可以预约
     */
  10 : bool appointmentAble,
   /**
   * 创建时间
   */
  11 : i64 createdAt,
   /**
   *
   */
    12 : i32 createdBy,
    /**
    * 更新时间
    */
    13 : i64 updatedAt,
    /**
    *
    */
    14:  i32 updatedBy,
    /**
    * 备注
    */
    15 : optional string remark,
    /**医生信息*/
    16 : DoctorInfo doctorInfo
    /**
    *(已预约)患者信息
    */
    17: optional PatientInfo patientInfo,
     /**
    *(已预约)订单id
    */
    18: optional i32 orderId,
    /**
    * 排班类型
    **/
    19:  order_enum.AppointmentType appointmentType,
    /**
    * 排班类型
    **/
    20:  order_enum.TimeInterval timeInterval,
}
struct ListAppointmentsByDoctorIdResponse{
     /**
    *  预约单列表
    **/
    1: list<AppointmentItem> appointments,
}

/*排班时间表*/
struct AppointmentTimeItem{
    /**排班时间id*/
   1:i32 id,
   /**医生排班开始时间*/
   2:i64 appointmentBeginTime,
   /**医生排班结束时间*/
   3:i64 appointmentEndTime,
   /**医生id*/
   4:i32 doctorId,
   /**是否已排班（医生已领取排班）*/
   5:bool allotted,
    /**
   *是否有权限重新调整排班
    */
   6 : bool alterable,
       /**
       * 挂号费用
       */
   7 :  double registrationAmount,
    /**
      * 挂号费用
      */
   8 : double discountRegistrationAmount,
     /**
       * 费用折扣（几折，如8.8则为 8.8折）
       */
   9 :  double discount,
   /**
   * 排班类型
   **/
   10: order_enum.AppointmentType appointmentType,
    /**
    * 时间段
    **/
   11: order_enum.TimeInterval timeInterval,

}

struct DoctorHomePageNotice{
    /**医生id*/
    1:i32 doctorId,
    /**预约数*/
    2:i32 appointments,

}

struct PatientHomePageNotice{
    /**患者id*/
    1:i32 patientId,
    /**排队候诊数量*/
    2:i32 untreateds,
    /**诊断报告数量*/
    3:i32 diagnoses,
    /**未缴费订单数量*/
    4:i32 unpaids,
}

struct OrderForWxResponse{
    /* appid */
    1: string appid,
    /* partnerid */
    2: string partnerid,
    /* prepayid */
    3: string prepayid,
    /* package */
    4: string _package,
    /* noncestr */
    5: string noncestr,
    /* timestamp */
    6: string timestamp,
    /* sign */
    7: string sign,
    /** 微信下单是否成功 **/
    8: bool returnCode

}

struct OfflinePatientResponse{
    /**
    * id
    **/
    1: i32 id,
    /**
    * name
    **/
    2: string name,
    /**
    * phone
    **/
    3: string phone,
    /**
    * 性别
    **/
    4: order_enum.Gender gender,
    /**
    *  年龄
    **/
    5: i32 age,
}


struct PatientHomePageAppointment{
    /**医生id*/
    1:i32 doctorId,
    /**医生名字*/
    2:optional string name,
    /**医生电话*/
    3:optional string phone,
    /**
    * 医生头像
    */
    4: optional string headImage,
     /**
    * 医生职称
    **/
    5: optional string jobTitle,
    /**
    * 医生级别
    **/
    6: i32 level,

    /**特长标签*/
    7:optional string  labels,
    /**
    * 从业经历
    **/
    8: optional string jobExperience,

    /**
    * 所属医院名称
    **/
    9:optional string hospitalName,

    /**
    所属部门名称
    **/
    10: optional string departmentName,
    /**
    *  预约类型
    */
    13: optional  order_enum.AppointmentType appointmentType,
    /**
    *  医生提醒描述信息
    */
    14: optional  string docInfo,

}
struct PatientHomePageArticle {
    /**
    * 标题
    **/
    1: i32 articleId
    /**
    * 标题
    **/
    2: string title
    /**
    * 阅读人数
    **/
    3: optional i32 num
    /**
    * 描述
    **/
    4: optional string desc
    /**
    * 跳转地址
    **/
    5: optional string destUrl
    /**
    * 文章类型
    **/
    6: optional order_enum.ArticleTypeEnum articleType
}

struct XZHelperItem {
    /**
    * 标题
    **/
    1: string title
    /**
    * 描述
    **/
    2: optional string desc
    /**
    * 图片地址
    **/
    3: optional string imageUrl
    /**
    * 跳转地址
    **/
    4: optional string destUrl

}

struct PatientHomePageInfoResponse{
    /**
    *  推荐的首诊医生
    **/
    1: optional PatientHomePageAppointment firstDoctor
    /**
    *  我的主治医生（最近有预约）
    **/
    2: optional PatientHomePageAppointment appointment
    /**
    *  我的主治医生 (等同于第一次预约的医生)
    **/
    3: optional PatientHomePageAppointment attendingDoctor
    /**
    *  首次推荐的治疗师
    **/
    4: optional PatientHomePageAppointment firstPsychologist
    /**
    *  我的理疗师
    **/
    5: optional PatientHomePageAppointment lastPsychologist
    /**
    *  我的理疗师（最近有预约）
    **/
    6: optional PatientHomePageAppointment psychologistAppointment
    /**
    /**
    * 推荐文章信息
    **/
    7: optional PatientHomePageArticle article
    /**
    * 心知助手（包含推荐的量表信息）
    **/
    8: optional list<XZHelperItem> xinzhiHelperArticle
}

struct SliderNoticeItem{
    /**
    * 用户手机号
    **/
    1: string patientPhone
    /**
    * 通知类型
    **/
    2: order_enum.SliderNoticeType sliderNoticeType
    /**
    * 医生姓名
    **/
    3: optional string doctorName
    /**
    * 量表名称
    **/
    4: optional string titleName
    /**
    * 通知时间
    **/
    5: i64 createdAt
}
struct PatientHomePageSliderNoticeResponse{
   1: list<SliderNoticeItem> noticeItemList
}

/** 评测量表信息 **/
struct TitleCategoryInfo{
   /**
   * 评测量表类型
   */
 1 :  string category,
   /**
   * 量表中文名
   */
 2 :  string name,
   /**
   * 对应患症标签空格分割
   */
 3 : optional string labels,
   /**
   * 描述
   */
 4 : optional  string desc,
   /**
   * 测试次数
   */
 5 : optional  i32 testCount,
   /**
   * 备注
   */
 6 : optional string remark,

}

struct RecommendItem {
    /**
    * id
    **/
    1: i32 id,
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
    4: order_enum.RecommendTypeEnum recommendType,
    /**
    * 推荐的医生id
    **/
    5: optional i32 recDoctorId,
    /**
    * 推荐的量表类型
    **/
    6: optional string recTitleCategory,
    /**
    * 推荐人(医生)id
    **/
    7: i32 recommendBy,
    /**
    * 推荐医生详情
    **/
    8: optional DoctorInfo recDoctorInfo
    /**
    * 治疗费
    **/
    9: optional double price
    /**
    * 折扣价
    **/
    10: optional double discountPrice
    /**
    * 单位可治疗时间(分钟)
    **/
    11: optional i32 unitTime
    /**
    * 最近是否可预约
    **/
    12: optional bool isWorkable
    /**
    * 量表详情
    **/
    13: optional  TitleCategoryInfo titleCategoryInfo

}

/**推荐信息*/
struct ListRecommendsRespone {
    /**推荐分页响应包*/
   1: optional common_domain.PageResponse pageResponse,
    /*推荐列表**/
   2:list<RecommendItem> recommendList,
}
struct PriceRuleItem {
     /**医生等级*/
    1: i32 level
     /**医院Id*/
    2: i32 hospitalId
     /**医院名称*/
    3: optional string hospitalName
     /**排班类型*/
    4: order_enum.AppointmentType appointmentType
     /**价格*/
    5: double price
     /**折扣价*/
    6: double discountPrice
     /**备注*/
    7: optional string remark
}

struct ListPriceRuleResponse {
    /**推荐分页响应包*/
    1: optional common_domain.PageResponse pageResponse,
    /*价格规则列表**/
    2:list<PriceRuleItem> priceRuleList,
}

struct PackageChargeItem {
       /**医生ID*/
      1: i32 doctorId,
       /**
      *  套餐类型
      */
      2 : order_enum.AppointmentType appointmentType,
       /** 套餐/项目名称*/
      3: string packageName,
       /** 套餐/项目描述*/
      4: string packageDesc,
       /** 时间、价格说明*/
      5: string timeAndPriceDesc,
       /** 优惠价格*/
      6: double discountPrice,
      /** 是否已购买（用来判断是否购买来图文咨询包）*/
      7:bool bought,
      /**是否正在销售的，否则表示没有排班或者已停止销售*/
      8:bool selling,
      /** 订单id*/
      9: optional i32 orderId,
 }
 /**
 * 查询医生套餐请求包
 **/
 struct GetPackageInfoResponse{
    /** 医生ID*/
    1: i32 doctorId,
    /**
    *  套餐类型
    */
    2 : order_enum.AppointmentType appointmentType,
    /** 优惠价格*/
    3: double discountTherapyAmount,
    /**
    *  开始时间
    */
    4 :  i64 therapyBeginTime,
    /**
    *  结束时间
    */
    5 :  i64 therapyEndTime,

 }

 struct CountBuyTextPackPatientsResponse{
      /*图文包月患者总数**/
     1:i32 textPackPatient,

       /*新增图文包月患者总数**/
     2:i32 newAddTextPackPatient
 }

 /**判断是否可以聊天对话响应包*/
 struct JudgeChatAbleResponse{
   /**是否可以聊天*/
   1: bool chatable,
   /**订单id*/
   2: optional i32 orderId,
   /**
   *  开始时间
   */
   3 : optional i64 beginTime,
   /**
   *  结束时间
   */
   4 : optional i64 endTime,
  /** 患者信息*/
   5:optional PatientInfo patientInfo,
  /** 医生信息*/
   6:optional DoctorInfo doctorInfo,
 }

 struct ListUseableCouponsResponse {

 }

 /** 心知陪护订单的下单参数 */
 struct AccompanyOrderParam{

    /**
    * 心知陪护的套餐类型、天数
    **/
    1: order_enum.AccompanyPackEnum accompanyPackEnum,

    /**价格*/
    2: double price
     /**折扣价*/
    3: double discountPrice

    /**
    * 订单结束时间
    **/
    4: i64 therapyEndTime,

 }

  /** 心理保健套餐（优惠券）订单的下单参数 */
  struct CouponOrderParams {
     /**购买价格*/
     1: double discountPrice,
      /**券的张数*/
     2: i32 quantity,
     /**券的面值*/
     3: double faceValue,
     /**
     * 失效时间
     **/
     4: i64 expiredAt,
      /**（已购买）剩余数量*/
     5: i32 remain,
  }


 struct GetPatientCaregiverHomePageInfoResponse{
    /**
    * 在线客服信息
    **/
    1: optional DoctorInfo doctorInf,
    /**
    * 剩余陪护天数
    **/
    2: i32 dayNum,
    /**
    * 新消息数
    **/
    3: i32 msgNum,
    /**
    * 预约数
    **/
    4: i32 appointmentNum,
    /**
    * 标识是否购买过陪护套餐
    **/
    5: bool accompanyPackFlag,
    /**
    * 默认对话文本信息
    **/
    6: optional string replyCount,
    /**
    * 默认对话富文本信息
    **/
    7: optional string replyUrltext,
    /**
    * 默认对话富文本信息链接
    **/
    8: optional string replyUrl,

 }

 /* 体验码 */
 struct ExperienceCoupon{
    /**
    * id
    **/
    1: i32 id,
    /**
    * code
    **/
    2: string code,
    /**
    * 订单Id
    **/
    3: i32 orderId,
    /**
    * 用户Id
    **/
    4: i32 customerId,
    /**
    * 发行时间,预留字段
    **/
    5: i64 publish_at,
    /**
    * 过期时间,预留字段
    **/
    6: i64 expired_at,
    /**
    * 状态
    **/
    7: order_enum.TCouponStatus status,
 }

 struct ListExperienceCouponResponse {
    /**
    *  体验码列表
    **/
    1: list<ExperienceCoupon> experienceCoupon,
    /**
    *  分页查询响应
    **/
    2: common_domain.PageResponse pageResponse,
 }