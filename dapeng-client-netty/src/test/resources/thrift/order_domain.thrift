namespace java com.github.dapeng.json.demo.domain
include "order_enum.thrift"
struct TOrderDTO{
    /**
    * 订单 ID
    */
  1 :  i32 id,
    /**
    * 父订单id
    */
  2 : optional i32 parentId,
    /**
    * 医生 ID
    */
  3 :  i32 doctorId,
    /**
    * 客户 ID
    */
  4 :  i32 patientId,
    /**
    * 患者补充的症状描述
    */
  5 : optional string patientInformationContent,
    /**
    * 诊断信息ID
    */
  6 : optional i32 diagnoseId,
    /**
    * 预约信息 ID
    */
  7 :  i32 appointmentId,
    /**
    * 订单状态,1:新建[已挂号但未缴纳诊费](init);2:待诊断[已缴纳就诊费用](waiting_diagnosis);3:已过期[挂号后没在设定时间缴纳就诊费用](expired);4:已诊断[正常诊断完毕](confirmed_diagnosis);5:已退款[异常情况导致退诊断费用](refund);6:已取消[取消订单](cancel);7:已发起通话[患者未接通](launched);8:诊断中(diagnosing);9:已过期未就诊[医生发起过通话但未接通](patient_expired);10:已过期未诊断[医生没有发起会话](doctor_expired);11:超时付款成功(expired_paid);12:异常取消(exception_cancel)
    */
  8 :  i32 status,
    /**
    * 治疗费
    */
  9 :  double therapyAmount,
    /**
    * 折扣后治疗费
    */
  10 :  double discountTherapyAmount,
    /**
    * 缴费过期时间
    */
  11 :  i64 registrationExpireTime,
    /**
    *  预约诊断开始时间
    */
  12 :  i64 therapyBeginTime,
    /**
    *  预约诊断结束时间
    */
  13 :  i64 therapyEndTime,
    /**
    * 诊疗方式,1:APP(app);2:电话(phone);3:面对面(face2face)
    */
  14 :  i32 therapyMode,
    /**
    * 排班类型,1:首诊(first_visit);2:复诊(return_visit);3:预约治疗(treat)
    */
  15 :  i32 appointmentType,
    /**
    * 医生结算分成比例,记录的是百分比
    */
  16 :  double devidedRate,
    /**
    * 是否是首诊
    */
  17 :  i32 firstTherapy,
    /**
    * 创建时间
    */
  18 :  i64 createdAt,
    /**
    *
    */
  19 :  i32 createdBy,
    /**
    * 更新时间
    */
  20 :  i64 updatedAt,
    /**
    *
    */
  21 :  i32 updatedBy,
    /**
    *
    */
  22 :  string remark,
    /**
    * 支付平台的唯一订单号
    */
  23 :  string transactionId,
    /**
    * 预约订单的唯一订单号
    */
  24 :  string orderNum,
    /**
    * 支付类型,1:支付宝(alipay);2:微信(wechat);3:线下(offline);4:免费(free)
    */
  25 :  i32 payType,
    /**
    * 治疗提醒消息发送状态,0:未发送;1:2小时提醒已发送;2:1小时提醒已发送;3:30分钟提醒已发送
    */
  26 :  i32 remindStatus,
    /**
    * 线下患者ID
    */
  27 :  i32 offlinePatientId,
    /**
    * 医生是否已阅读
    */
  28 :  bool read,
   /**
      * 使用抵扣券金额
      */
   29 :double couponAmount,

}

struct TAppointmentDTO{
   /**
   * 排班ID
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
    * 挂号费用
    */
 7 :  double discountRegistrationAmount,
   /**
   * 预约状态,1:可领取[医生可以排班的](available);2:可预约(workable);3:已预约(reserved);4:已过期[当天没人预约就置为过期](refund)
   */
  8 :  i32 status,
   /**
   * 创建时间
   */
  9 : i64 createdAt,
   /**
   *
   */
  10 : i32 createdBy,
   /**
   * 更新时间
   */
  11 : i64 updatedAt,
   /**
   *
   */
 12:  i32 updatedBy,
   /**
   *
   */
 13 :  string remark,
  /**
    *排班时间id
    */
 14:i32 appointmentTimeId,
  /**
    * 排班类型,1:首诊(first_visit);2:复诊(return_visit);3:预约治疗(treat)
    */
  15 :  i32 appointmentType,
}

struct TDiagnoseDTO{
   /**
     * ID
     */
   1 :  i32 id,
     /**
     * 医生 ID
     */
   2 :  i32 doctorId,
     /**
     * 客户 ID
     */
   3 :  i32 patientId,
     /**
     * 是否已阅读
     */
   4 : optional i32 read,
     /**
     * 诊断信息
     */
   5 :  string content,
     /**
     * 创建时间
     */
   6 :  i64 createdAt,
     /**
     *
     */
   7 :  i32 createdBy,
     /**
     * 更新时间
     */
   8 :  i64 updatedAt,
     /**
     *
     */
   9 :  i32 updatedBy,
     /**
     *
     */
   10 :  string remark,
}
struct TPriceRuleDTO {
     /**医生等级*/
    1: i32 level
     /**医院Id*/
    2: i32 hospitalId
     /**排班类型*/
    3: order_enum.AppointmentType appointmentType
     /**价格*/
    4: double price
     /**折扣价*/
    5: double discountPrice
     /**备注*/
    6: optional string remark
}

struct TCouponDTO{
   /**
   *
   */
 1 :  i32 id,
    /**
    *订单id
    */
  2 :optional  i32 orderId,
   /**
   * 优惠券类型,1:优惠券(discount);2:心理咨询优惠券[仅限于预约心理咨询](appointment)
   */
  3 :  order_enum.TCouponType type,
   /**
   * 优惠券面值
   */
  4 :  double amount,
   /**
   * 发行时间
   */
 5 :  i64 publishAt,
   /**
   * 过期时间
   */
 6 :  i64 expiredAt,
   /**
   * 患者id
   */
 7 : optional i32 patientId,
   /**
   * 状态,0:无效(invalid);1:新发(available);2:已使用(used);3:已过期(expired)
   */
 8 :  order_enum.TCouponStatus status,
   /**
   *
   */
 9 :  i64 createdAt,
   /**
   *
   */
 10 :  i32 createdBy,
   /**
   *
   */
 11 :  i64 updatedAt,
   /**
   *
   */
 12 :  i32 updatedBy,
}
