namespace java com.github.dapeng.json.demo.enums
enum QueryAppointmentFromEnum {

    /**
    * 来自医生端
    **/
    DOCTOR = 1,
    /**
    * 来自病人端端
    **/
    PATIENT = 2,
    /**
     * 来自管理系统
     **/
    OSS = 3,
}

/**
*订单支付方式
**/
 enum TOrderPayType{
   /**
   *支付宝
   **/
   ALIPAY = 1,
   /**
   *微信
   **/
   WECHAT = 2,
   /**
   * 线下
   **/
   OFFLINE = 3,
   /**
  * 免费
  **/
  FREE = 4,

}
/**
*订单状态
**/
enum TOrderStatus{
/**
   *新建[已挂号但未缴纳诊费]
   **/
   INIT=1,

   /**
   *待诊断[已缴纳就诊费用]
   **/
   WAITING_DIAGNOSIS=2,

   /**
   *已过期[挂号后没在设定时间缴纳就诊费用]
   **/
   EXPIRED=3,

   /**
   *已诊断[正常诊断完毕]
   **/
   CONFIRMED_DIAGNOSIS=4,

   /**
   *已退款[异常情况导致退诊断费用]
   **/
   REFUND=5,

   /**
   *已取消[取消订单]
   **/
   CANCEL=6,

   /**
   *已发起通话[患者未接通]
   **/
   LAUNCHED=7,

   /**
   *诊断中
   **/
   DIAGNOSING=8,

   /**
   *已过期未就诊[医生发起过通话但未接通]
   **/
   PATIENT_EXPIRED=9,

   /**
   *已过期未诊断[医生没有发起会话]
   **/
   DOCTOR_EXPIRED=10,

   /**
   * 超时付款成功[待退款]
   **/
   EXPIRED_PAID=11,
   /**
    * 异常取消
   **/
   EXCEPTION_CANCEL=12,

}

/**
*医生排班预约状态
**/
enum TAppointmentStatus{
   /**
   * 可领取[医生可以排班的]
   **/
   AVAILABLE=1,
 /**
   *可预约
   **/
   WORKABLE=2,

   /**
   *已预约
   **/
   RESERVED=3,

   /**
   *已过期[当天没人预约就置为过期]
   **/
   REFUND=4

}

/**
*诊疗方式
**/
enum TOrderTherapyMode{
/**
   *APP
   **/
   APP=1,

   /**
   *电话
   **/
   PHONE=2,

   /**
   *面对面
   **/
   FACE2FACE=3
}

/**
* 预约周期
**/
enum AppointmentPeriod{
 /**
   * 一个星期内
   **/
   ONE_WEEK = 1,
   /**
   * 2个星期内
   **/
   TWO_WEEK = 2,
   /**
   * 3个星期内
   **/
   THREE_WEEK = 3,
   /**
   * 1个月内
   **/
   ONE_MONTH = 4,
   /**
   * 2个月内
   **/
   TWO_MONTH = 5,
   /**
   * 3个月内以及3个月以上
   **/
   THREE_MONTH = 6,
    /**
   * 上一次预约
   **/
   LAST = 7,
    /**
    * 没有排班
    **/
    NONE = 8,
}

/**
* 线下患者性别
**/
enum Gender{
    /**
    * 女
    **/
    FEMALE = 0,
    /**
    * 男
    **/
    MALE = 1
}

/**
*  排班类型
**/
enum AppointmentType{
    /**
    * 首诊
    **/
    FIRST_VISIT = 1,
    /**
    * 复诊
    **/
    RETURN_VISIT = 2,
    /**
    * 预约治疗
    **/
    TREAT = 3,
    /**
    * 图文月套餐包
    **/
    TEXT_PACK = 4,
    /**
    * 知心陪护套餐
    **/
    ACCOMPANY_PACK = 5,
    /**
    * 心理保健套餐（优惠券）
    **/
    TREAT_COUPON = 6,
}

/**
*  时段：早上、下午、晚上
**/
enum TimeInterval{
    /**
    * 早上
    **/
    MORNING = 1,
    /**
    * 下午
    **/
    AFTERNOON = 2,
    /**
    * 晚上
    **/
    NIGHT = 3,
}

/**
*  时段：早上、下午、晚上
**/
enum SliderNoticeType{
    /**
    * 首诊
    **/
    FIRST_VISIT = 1,
    /**
    * 复诊
    **/
    RETURN_VISIT = 2,
    /**
    * 预约治疗
    **/
    TREAT = 3,
    /**
    * 量表测试
    **/
    TITLE_TEST = 4,
    /**
    * 登陆请求
    **/
    LOGIN = 5,
}

/**
* 推荐类型
**/
enum RecommendTypeEnum {
    /**
    * 量表推荐
    **/
    REC_TABLE = 1,
    /**
    * 心理治疗师医生推荐
    **/
    REC_DOCTOR = 2,
}
/**
* 推荐文章类型
**/
enum ArticleTypeEnum {
    /**
    * 抑郁症
    **/
    AC_SDS = 1,
    /**
    * 焦虑症
    **/
    AC_SAS = 2,
    /**
    * 失眠症
    **/
    AC_AIS = 3,
    /**
    * 精神分裂症
    **/
    AC_PSQI = 4,
    /**
    * 强迫症
    **/
    AC_QP = 5,
    /**
    * 其他
    **/
    AC_OTHER = 99
}

/**
*  知心陪护 订单类型
**/
enum AccompanyPackEnum{
    /**
    * 非知心陪护的订单，默认值为0
    **/
    NONE = 0,

    /**
    * 月套餐
    **/
    MONTH = 30,

    /**
    * 季套餐
    **/
    SEASON = 90,
    /**
    * 体验套餐
    **/
    WEEK = 7,

}


/**
*优惠券类型
**/
enum TCouponType{
   /**
   *优惠券
   **/
   DISCOUNT=1,

   /**
   *心理咨询优惠券[仅限于预约心理咨询]
   **/
   APPOINTMENT=2,
}

/**
*状态
**/
enum TCouponStatus{
   /**
   *无效
   **/
   INVALID=0,

   /**
   *可用的
   **/
   AVAILABLE=1,

   /**
   *已使用
   **/
   USED=2,

   /**
   *已过期
   **/
   EXPIRED=3,
}
