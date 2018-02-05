namespace java com.github.dapeng.json.demo.service
include "order_request.thrift"
include "order_domain.thrift"
include "order_response.thrift"
include "common_domain.thrift"


service OrderService{
/**
###
#### 业务描述
   1、 批量修改单个医生一天排班情况
   2、如果以排班的时间去除勾选（弃用）则删除排班数据
   3、如果原来未排班的数据选中（认领）后则创建一天可预约(workable)记录

#### 接口依赖

#### 边界异常说明
    无

#### 输入
    1. 选中和非选中的时间段均要上传，否则未上送的时间号段不做处理

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理

####  输出
    无
    */
    void batchAppointment(/**保存排班请求*/1:order_request.BatchAppointmentRequest request),


/**
### 医生排班
#### 业务描述
   1、 批量修改单个医生一天排班情况
   2、如果以排班的时间去除勾选（弃用）则删除排班数据
   3、如果原来未排班的数据选中（认领）后则创建一天可预约(workable)记录

#### 接口依赖
    listAppointmentTimeForDoctor
#### 边界异常说明
    无

#### 输入
    1. 选中和非选中的时间段均要上传，否则未上送的时间号段不做处理

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理

####  输出
    返回当天的所有排班时间段
    */
      list<order_response.AppointmentTimeItem> batchAppointment4Doctor(/**保存排班请求*/1:order_request.BatchAppointmentRequest request),
/**
### 条件查询排班列表
#### 业务描述
    添加医生排班

#### 接口依赖

#### 边界异常说明
    无

#### 输入
    1.查询条件

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
    1.排班列表
    */
    order_response.ListAppointmentsResponse listAppointments(/**查询条件*/1:order_request.ListAppointmentsRequest request),

/**
### 批量创建【可领取】的排班信息
#### 业务描述
    添加医生排班

#### 接口依赖

#### 边界异常说明
    无

#### 输入
    1.创建条件

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
   1. 无
    */
    void createAppointmentForAvailable(/**条件*/1:order_request.CreateAppointmentForAvailableRequest request),
void createAppointmentForAvailable1(/**条件*/1:order_request.CreateAppointmentForAvailableRequest1 request),

/**
### 最后的诊断医生
#### 业务描述
    最后的诊断医生

#### 接口依赖

#### 边界异常说明
    无

#### 输入
    1.病人id

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
    1.
    */
     order_response.GetLastTreatDoctorResponse getLastTreatDoctor(/**病人id*/i32 patientId),

     /**
     ### 创建订单
     #### 业务描述
         预约锁定医生（创建【预约】订单，补充病人信息， 症状记录到订单表）

     #### 接口依赖

     #### 边界异常说明
         无

     #### 输入
         1.病人id

     #### 前置检查


     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
         1. OrderItem
         */
   order_response.OrderItem createOrder(/**创建订单请求*/1:order_request.CreateOrderRequest request),

   /**
   ### 查询订单列表
   #### 业务描述
       查询订单列表

   #### 接口依赖

   #### 边界异常说明
       无

   #### 输入
       1.查询条件

   #### 前置检查


   ####  权限检查

   ####  逻辑处理


   #### 数据库变更

   ####  事务处理


   ####  输出
       1.
       */
    order_response.ListOrderResponse listOrders(/**查询条件*/1:order_request.ListOrderRequest request),
     /**
       ### 查询订单明细
       #### 业务描述
           查询订单明细

       #### 接口依赖

       #### 边界异常说明
           无

       #### 输入
           1.查询条件

       #### 前置检查


       ####  权限检查

       ####  逻辑处理


       #### 数据库变更

       ####  事务处理


       ####  输出
           1.
           */
        order_response.OrderItem getOrder(/**查询条件*/1:i32 orderId),

    /**
       ### 修改订单状态
       #### 业务描述
           修改订单状态

       #### 接口依赖

       #### 边界异常说明
           无

       #### 输入
           1.查询条件

       #### 前置检查


       ####  权限检查

       ####  逻辑处理


       #### 数据库变更

       ####  事务处理


       ####  输出
           1.
    */
    void  modifyOrderStatus(1:order_request.ModifyOrderStatusRequest request),

    /**
           ### 订单缴费成功的回调接口
           #### 业务描述
               订单缴费支付的回调接口

           #### 接口依赖

           #### 边界异常说明
               无

           #### 输入
               1. 订单唯一ID
               2. 支付平台的订单ID

           #### 前置检查
               1. 检查订单是否存在

           ####  权限检查

           ####  逻辑处理
               1. 更新订单状态并记录支付平台的订单ID
               2. 调用结算接口，创建结算记录

           #### 数据库变更

           ####  事务处理


           ####  输出
               1.
        */
    bool payNotify(/*请求体*/1: order_request.PayNotifyRequest payNotifyRequest),

    /**
           ### 创建诊疗报告（医生端）
           #### 业务描述
               创建诊疗报告

           #### 接口依赖
                saveRecommends
           #### 边界异常说明
               无

           #### 输入
               1. 订单ID
               2. 诊疗报告内容
               3、如果是首诊报告还有推荐医生和推荐量表
           #### 前置检查


           ####  权限检查

           ####  逻辑处理
               1. 插入数据库

           #### 数据库变更

           ####  事务处理


           ####  输出
               1. id
        */
    i32 createDiagnose(/*请求体*/1: order_request.CreateDiagnoseRequest createDiagnoseRequest),

    /**
       ### 诊疗报告列表
       #### 业务描述
           诊疗报告列表，支持医生和患者查自己的报告

       #### 接口依赖

       #### 边界异常说明
           无

       #### 输入


       #### 前置检查


       ####  权限检查

       ####  逻辑处理


       #### 数据库变更

       ####  事务处理


       ####  输出
           1. id
    */
    order_response.ListDiagnoseResponse listDiagnoses(/*请求体*/1: order_request.ListDiagnoseRequest listDiagnoseRequest),


    /**
       ### 预约就诊计划(排队就诊)
       #### 业务描述
           查询未过期的预约记录

       #### 接口依赖

       #### 边界异常说明
           无

       #### 输入


       #### 前置检查


       ####  权限检查

       ####  逻辑处理


       #### 数据库变更

       ####  事务处理


       ####  输出
           1. id
    */
     order_response.ListTreatPlansResponse  listTreatPlans(),
/**
### 查询可以预约医生（按时间周期排序）
#### 业务描述
查询可以预约医生（按时间周期排序）

#### 接口依赖

#### 边界异常说明
无

#### 输入


#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可预约医生列表
*/
      order_response.AppointmentDoctorsResponse  listAppointmentDoctors(1:order_request.ListAppointmentDoctorsRequest request),

/**
### 用医生姓名来查询可以预约医生（按时间周期排序）
#### 业务描述
用医生姓名来查询可以预约医生（按时间周期排序）

#### 接口依赖

#### 边界异常说明
无

#### 输入


#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可预约医生列表
*/
      order_response.AppointmentDoctorsResponse  listAppointmentForDoctorName(1: order_request.ListAppointmentForDoctorNameRequest listAppointmentForDoctorNameRequest),


/**
### 按日期查询可以预约医生列表
#### 业务描述
按医生查询可以预约列表

#### 接口依赖

#### 边界异常说明
无

#### 输入
1、医生id
2、日期

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可预时间列表
*/
    order_response.AppointmentDoctorsByDateResponse  listAppointmentDoctorsByDate(/**查询日期:格式 yyyy-MM-dd */1:order_request.AppointmentDoctorsByDateRequest request),

/**
### 医生查询已预约的患者列表
#### 业务描述
医生查询已预约的患者列表

#### 接口依赖

#### 边界异常说明
无

#### 输入
1、订单状态 hasTreated ：是否已经治疗 true：已治疗 false :未治疗


#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 患者列表
*/
order_response.ListAppointmentPatientResponse  listAppointmentPatients(1:order_request.ListAppointmentPatientRequest request),

/**
### 查询(今日)值班医生列表
#### 业务描述
查询值班医生列表

#### 接口依赖

#### 边界异常说明
无

#### 输入

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 医生列表
*/
order_response.ListDoctorOnDutyResponse listDoctorOnDuty(/*分页*/1:common_domain.PageRequest pageRequest),

/**
### 预约日历
#### 业务描述
当前一个月预约日历

#### 接口依赖

#### 边界异常说明
无

#### 输入

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可以预约日期
*/
list<string>  listAppointmentCalendar(order_request.ListAppointmentCalendarRequest request),
/**
### 医生领取排班的日历
#### 业务描述
当前一个月排班日历

#### 接口依赖

#### 边界异常说明
无

#### 输入

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可以预约日期
*/
list<string>  listAppointmentCalendarForDoctor(),
/**
### 查询可以预约医生（按时间周期排序）
#### 业务描述
查询可以预约医生（按时间周期排序）

#### 接口依赖

#### 边界异常说明
无

#### 输入


#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可预约医生列表
*/
      order_response.ListAppointmentsByDoctorIdResponse  listAppointmentsByDoctorId(1:order_request.ListAppointmentsByDoctorIdRequest request),

      /**
      ### 查询排班记录信息
      #### 业务描述
      查询排班记录信息

      #### 接口依赖

      #### 边界异常说明
      无

      #### 输入
        appointmentId

      #### 前置检查

      ####  权限检查

      ####  逻辑处理


      #### 数据库变更

      ####  事务处理


      ####  输出
      1. 排班记录信息
      */
            order_response.AppointmentItem  getAppointmentItem(1:i32 appointmentId),

/**
### 查询医生排班记录信息(包括未安排的时间段)
#### 业务描述
1、查询医生排班记录信息(包括未安排的时间段)
2、医生排班管理列表

#### 接口依赖

#### 边界异常说明
无

#### 输入
appointmentId

#### 前置检查

####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 医生排班记录信息
*/
    list<order_response.AppointmentTimeItem>  listAppointmentTimeForDoctor(1:order_request.ListAppointmentTimeForDoctorRequest request),

    /**
    ### 医生首页通知
    #### 业务描述
    医生首页通知(各种待办记录的数量)

    #### 接口依赖

    #### 边界异常说明
    无

    #### 输入

    #### 前置检查

    ####  权限检查

    ####  逻辑处理


    #### 数据库变更

    ####  事务处理


    ####  输出
     1、我的预约： 已完成支付但没开始的订单数量
    */
   order_response.DoctorHomePageNotice getDoctorHomePageNotice(),

     /**
       ### 患者首页通知
       #### 业务描述
       患者首页通知(各种待办记录的数量)

       #### 接口依赖

       #### 边界异常说明
       无

       #### 输入

       #### 前置检查

       ####  权限检查

       ####  逻辑处理


       #### 数据库变更

       ####  事务处理


       ####  输出
       1、排队候诊 该用户已完成支付但没开始治疗的订单数量
       2、诊断报告： 未读的诊断报告数量
       3、订单缴费： 已创建但没支付的订单数量
       */
      order_response.PatientHomePageNotice getPatientHomePageNotice(),

   /**
     ### 阅读诊断报告
     #### 业务描述
     阅读诊断报告

     #### 接口依赖

     #### 边界异常说明
     无

     #### 输入
      1、排队候诊 该用户已完成支付但没开始治疗的订单数量
     #### 前置检查

     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
     1. 患者首页通知
     */

    void readDiagnose(/**诊断报告id*/1:i32 diagnoseId),

    /**
     ### 获取诊断报告
     #### 业务描述
     获取一份诊断报告

     #### 接口依赖

     #### 边界异常说明
     无

     #### 输入

     #### 前置检查

     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
     1. 患者首页通知
     */

    order_response.DiagnoseItem getDiagnoseItem(/**诊断报告id*/1:i32 diagnoseId),

    /**
     ### 获取订单的微信支付对象
     #### 业务描述
     获取订单的微信支付对象

     #### 接口依赖

     #### 边界异常说明
     无

     #### 输入

     #### 前置检查

     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
     1. 支付对象
     */

    order_response.OrderForWxResponse getOrderForWx(/**订单ID*/1:i32 orderId),

    /**
     ### 微信回调处理方法
     #### 业务描述
     微信回调处理方法

     #### 接口依赖

     #### 边界异常说明
     无

     #### 输入

     #### 前置检查

     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
     1. 微信回调的XML字符串
     */

    string payNotifyForWx(/** orderNum */ 1: string wxNotifyString),

    /**
     ### 获取订单的支付宝支付字符串
     #### 业务描述
     获取订单的支付宝支付字符串

     #### 接口依赖

     #### 边界异常说明
     无

     #### 输入

     #### 前置检查

     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
     1. 支付字符串
     */

    string getOrderForAlipay(/**订单ID*/1:i32 orderId),

    /**
     ### 支付宝回调处理方法
     #### 业务描述
     支付宝回调处理方法

     #### 接口依赖

     #### 边界异常说明
     无

     #### 输入

     #### 前置检查

     ####  权限检查

     ####  逻辑处理


     #### 数据库变更

     ####  事务处理


     ####  输出
     1. 支付宝回调的字符串
     */

    string payNotifyForAlipay(/** 回调参数 */ 1: map<string, string> params),

    /**
    ### 结束诊断会话
    #### 业务描述
     结束诊断会话，把订单状态修改为诊断结束
    #### 接口依赖

    #### 边界异常说明
    无

    #### 输入

    #### 前置检查


    ####  权限检查

    ####  逻辑处理


    #### 数据库变更

    ####  事务处理

    ####  输出

    */
    void diagnoseFinish(1:order_request.DiagnoseFinishRequest request),

/**
    ### 重做异常单
    #### 业务描述
     1、因排班问题或者是诊断是通话不顺畅需要重约时间再会诊

    #### 接口依赖

    #### 边界异常说明
    无

    #### 输入

    #### 前置检查
     1、需要已支付的预约订单才能购重新预约

    ####  权限检查

    ####  逻辑处理
     1、重新预约的订单直接跳过支付流程到等待就诊状态（但保留支付类型和金额信息），并且关联原订单(parent_id)
     2、原订单状态置为 12:异常取消(exception_cancel)

    #### 数据库变更

    ####  事务处理

    ####  输出

    */
     order_response.OrderItem redoExceptionOrder(1:order_request.RedoExceptionOrderRequest request),

    /**
    ### 添加线下患者
    #### 业务描述
     添加线下患者
    #### 接口依赖

    #### 边界异常说明
    无

    #### 输入

    #### 前置检查


    ####  权限检查

    ####  逻辑处理


    #### 数据库变更

    ####  事务处理

    ####  输出

    */
    i32 createOfflinePatient(1:order_request.CreateOfflinePatientRequest request),

    /**
    ### 获取线下患者信息
    #### 业务描述
     获取线下患者信息
    #### 接口依赖

    #### 边界异常说明
    无

    #### 输入
        1. id 或 手机号码 二选一

    #### 前置检查


    ####  权限检查

    ####  逻辑处理


    #### 数据库变更

    ####  事务处理

    ####  输出
       1. 线下患者信息
    */
    order_response.OfflinePatientResponse getOfflinePatient(1:order_request.GetOfflinePatientRequest request),

    /**
    ### 添加线下订单服务
    #### 业务描述
     添加线下订单服务
    #### 接口依赖

    #### 边界异常说明
    无

    #### 输入

    #### 前置检查


    ####  权限检查

    ####  逻辑处理


    #### 数据库变更

    ####  事务处理

    ####  输出

    */
    i32 createOrderForOffline(1:order_request.CreateOrderForOffLineRequest request),

   /**
   ### 编辑线下订单
   #### 业务描述
    编辑线下订单服务
   #### 接口依赖

   #### 边界异常说明
    无

   #### 输入

   #### 前置检查


   ####  权限检查

   ####  逻辑处理


   #### 数据库变更

   ####  事务处理

   ####  输出

   */
   void modifyOrderForOffline(1:order_request.ModifyOrderForOfflineRequest request),


   /**
   ### 编辑线下订单状态
   #### 业务描述
    编辑线下订单状态的服务
   #### 接口依赖

   #### 边界异常说明
    无

   #### 输入

   #### 前置检查


   ####  权限检查

   ####  逻辑处理


   #### 数据库变更

   ####  事务处理

   ####  输出

   */
   void modifyOrderStatusForOffline(1:order_request.ModifyOrderStatusRequest request),


/**
### 患者端首页显示医生的相关推荐信息
#### 业务描述
查询患者端首页显示医生的相关推荐信息
#### 接口依赖

#### 边界异常说明
无

#### 输入

#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理

####  输出

*/
    order_response.PatientHomePageInfoResponse getPatientHomePageInfo(1:order_request.PatientHomePageInfoRequest request),

/**
### 患者端首页显示滚动通知信息
#### 业务描述
查询患者端首页显示滚动通知信息
#### 接口依赖
#### 边界异常说明
无
#### 输入
#### 前置检查
####  权限检查
####  逻辑处理
#### 数据库变更
####  事务处理
####  输出

*/
    order_response.PatientHomePageSliderNoticeResponse getPatientHomePageSliderNotice(1:order_request.PatientHomePageSliderNoticeRequest request),


/**
### 保存患者推荐信息
#### 业务描述
  保存患者症状
#### 接口依赖
#### 边界异常说明
#### 输入

#### 前置检查

#### 权限检查
#### 逻辑处理

#### 数据变更
   1.

#### 事务处理
#### 输出
   1.void
**/
    void saveRecommends(1:order_request.SaveRecommendsRequest request),

/**
### 查询患者推荐信息
#### 业务描述
  保存患者症状
#### 接口依赖
#### 边界异常说明
#### 输入

#### 前置检查

#### 权限检查
#### 逻辑处理

#### 数据变更
   1.

#### 事务处理
#### 输出
   1.void
**/
    order_response.ListRecommendsRespone listRecommends(1:order_request.ListRecommendsRequest request),

    /**
    ### 查询文章阅读数量
    #### 业务描述
      查询文章阅读数量
    #### 接口依赖
    #### 边界异常说明
    #### 输入
        articleId
    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
       1.

    #### 事务处理
    #### 输出
       1.文章阅读数量
    **/
        i32 getArticleReadership(1:i32 articleId),

    /**
    ### 阅读文章并返回阅读次数
    #### 业务描述
    阅读文章，文章阅读书+1
    #### 接口依赖
    #### 边界异常说明
    #### 输入
    articleId
    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
    1.

    #### 事务处理
    #### 输出
    1.文章阅读数量
    **/
    i32 readArticle(1:i32 articleId),
    /**
    ### 价格规则表列表查询
    #### 业务描述
   价格规则表列表
    #### 接口依赖
    #### 边界异常说明
    #### 输入
    articleId
    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
    1.

    #### 事务处理
    #### 输出
    1.价格规则表列表
    **/
    order_response.ListPriceRuleResponse listPriceRule(1:order_request.ListPriceRuleRequest request),
    /**
    ### 价格规则表修改
    #### 业务描述
   价格规则表列表
    #### 接口依赖
    #### 边界异常说明
    #### 输入
    articleId
    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
    1.

    #### 事务处理
    #### 输出
    void
    **/
    void modifyPriceRule(1:order_domain.TPriceRuleDTO request),
    /**
    ### 价格规则表添加
    #### 业务描述
    添加价格规则表
    #### 接口依赖
    #### 边界异常说明
    #### 输入
    articleId
    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
    1.

    #### 事务处理
    #### 输出
    void
    **/
    void savePriceRule(1:order_domain.TPriceRuleDTO request),


   /**
    ### 查询医生的诊疗套餐价格
    #### 业务描述
    查询医生的诊疗套餐价格，如果已购买了图文咨询套餐则返回已购买标志
    #### 接口依赖
    #### 边界异常说明
    #### 输入

    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
    1.

    #### 事务处理
    #### 输出
    void
    **/
    list<order_response.PackageChargeItem> listPackageChargesByDoctorId(1:i32 doctorId),

 /**
    ### 查询医生的诊疗套餐
    #### 业务描述
    #### 接口依赖
    #### 边界异常说明
    #### 输入

    #### 前置检查

    #### 权限检查
    #### 逻辑处理

    #### 数据变更
    1.

    #### 事务处理
    #### 输出
    void
    **/
    order_response.GetPackageInfoResponse getPackageInfo(1:order_request.GetPackageInfoRequest request),

    /**
        ### 购买诊疗套餐
        #### 业务描述
        #### 接口依赖
        #### 边界异常说明
        #### 输入

        #### 前置检查

        #### 权限检查
        #### 逻辑处理

        #### 数据变更
        1.

        #### 事务处理
        #### 输出
        void
        **/
    order_response.OrderItem buyPackage(1:order_request.BuyPackageRequest request)

    order_response.ListTextPackOrderForDoctorResponse listTextPackOrderForDoctor(1:order_request.ListTextPackOrderForDoctorRequest request)

    /**
        ### 查询订购了医生的图文咨询包月服务的患者数量
        #### 业务描述
           查询关注记录
        #### 接口依赖
        #### 边界异常说明
        #### 输入

        #### 前置检查

        #### 权限检查
         仅允许医生进行该操作
        #### 逻辑处理

        #### 数据变更
            1.

        #### 事务处理
        #### 输出

        **/
      order_response.CountBuyTextPackPatientsResponse countBuyTextPackPatients(1:i32 doctorId),


    /**
    ### 判断是否可以聊天对话
    #### 业务描述
       判断医生和患者是否可以聊天对话
       1、医生请求时，如果可以对话，返回可以对话时间以及患者信息
       2、患者请求时，如果可以对话，返回可以对话时间以及医生信息
    #### 接口依赖
    #### 边界异常说明
    #### 输入

    #### 前置检查

    #### 权限检查
     登陆的医生或者患者
    #### 逻辑处理

    #### 数据变更
        1.

    #### 事务处理
    #### 输出

     **/
     order_response.JudgeChatAbleResponse judgeChatAble(1: order_request.JudgeChatAbleRequest request),
        /**
         ### 发放优惠券
         #### 业务描述
         给患者发放优惠券
         #### 接口依赖
         #### 边界异常说明
         #### 输入

         #### 前置检查

         #### 权限检查
         #### 逻辑处理

         #### 数据变更
         1.

         #### 事务处理
         #### 输出
         1.
         **/
     void giveCoupon(1:order_request.GiveCouponRequest request),

        /**
         ### 批量发放优惠券
         #### 业务描述
         批量给患者发放优惠券
         #### 接口依赖
         #### 边界异常说明
         #### 输入

         #### 前置检查

         #### 权限检查
         #### 逻辑处理

         #### 数据变更
         1.

         #### 事务处理
         #### 输出
         1.
         **/
     void batchGiveCoupon(1:list<order_request.GiveCouponRequest> request),
         /**
          ### 查询客户可使用的优惠卷
          #### 业务描述
          查询客户可使用的优惠卷
          #### 接口依赖
          #### 边界异常说明
          #### 输入

          #### 前置检查

          #### 权限检查
          #### 逻辑处理

          #### 数据变更
          1.

          #### 事务处理
          #### 输出
          1.
          **/
      list<order_domain.TCouponDTO> listAvaliableCoupons(1:order_request.ListAvaliableCouponsRequest request),

    /**
    ### 查询最新的知心陪护订单
    #### 业务描述
       查询最新的知心陪护订单

    #### 接口依赖
    #### 边界异常说明
    #### 输入
       输入用户Id

    #### 前置检查

    #### 权限检查
     登陆的医生或者患者
    #### 逻辑处理

    #### 数据变更
        1.

    #### 事务处理
    #### 输出

     **/
    list<order_response.OrderItem> findAccompanyOrderByPatientId(1: i32 patientId)

    /**
    ### 创建知心陪护订单
    #### 业务描述
       创建的知心陪护订单

    #### 接口依赖
    #### 边界异常说明
    #### 输入
       输入用户Id

    #### 前置检查

    #### 权限检查
     登陆的医生或者患者
    #### 逻辑处理

    #### 数据变更
        1.

    #### 事务处理
    #### 输出

     **/
    order_response.OrderItem createAccompanyOrder(1: order_request.CreateAccompanyOrderRequest request)

    /**
    ### 获取知心陪护订单的参数
    #### 业务描述
       下单页面，获取知心陪护订单的参数

    #### 接口依赖
    #### 边界异常说明
    #### 输入


    #### 前置检查

    #### 权限检查

    #### 逻辑处理

    #### 数据变更

    #### 事务处理
    #### 输出
        知心陪护下单参数

     **/
    list<order_response.AccompanyOrderParam> getAccompanyOrderParams()

        /**
        ### 获取心理保健套餐（优惠券）订单的参数
        #### 业务描述
           下单页面，获取心理保健套餐（优惠券）订单的参数

        #### 接口依赖
        #### 边界异常说明
        #### 输入


        #### 前置检查

        #### 权限检查

        #### 逻辑处理

        #### 数据变更

        #### 事务处理
        #### 输出
           心理保健套餐（优惠券）下单参数

         **/
        order_response.CouponOrderParams getCouponOrderParams(),

    /**
    ### 获取知心陪护最新首页数据
    #### 业务描述
       获取知心陪护最新首页数据


    #### 接口依赖
    #### 边界异常说明
    #### 输入


    #### 前置检查

    #### 权限检查

    #### 逻辑处理

    #### 数据变更

    #### 事务处理
    #### 输出
       1.当前客服信息
       2.陪护结束日期
       3.消息数
       4.预约数
     **/
     order_response.GetPatientCaregiverHomePageInfoResponse getPatientCaregiverHomePageInfo(1:i32 patientId)

    /**
    ### 体验码 体验陪护七天
    #### 业务描述
       体验码 体验陪护七天

    #### 接口依赖
    #### 边界异常说明
    #### 输入


    #### 前置检查

    #### 权限检查

    #### 逻辑处理

    #### 数据变更

    #### 事务处理
    #### 输出
        知心陪护订单详情

     **/
    order_response.OrderItem createAccompanyOrderByCode(1: string code),

/**
### 治疗心理咨询师推荐
#### 业务描述
治疗心理咨询师推荐
 1、自动匹配一个最近可预约的咨询师
 2、如果用户以前预约过，优先配预约过的
 3、点击换一换，按咨询是最近可以预约时间依次替换
#### 接口依赖

#### 边界异常说明
无

#### 输入


#### 前置检查


####  权限检查

####  逻辑处理


#### 数据库变更

####  事务处理


####  输出
1. 可预约医生列表
*/
      order_response.AppointmentDoctorsResponse  recommendTreatDoctors(1: common_domain.PageRequest pageRequest),


    /**
    ### 创建体验码
    #### 业务描述
       创建体验码

    #### 接口依赖
    #### 边界异常说明
    #### 输入
        输入生成多少个体验码

    #### 前置检查

    #### 权限检查

    #### 逻辑处理
        遇到生成失败的，即体验码已存在的，忽略处理

    #### 数据变更

    #### 事务处理
    #### 输出
        成功生成多少个

     **/
    i32 createExperienceCoupon(1: i32 number)

    /**
    ### 体验码列表
    #### 业务描述
       体验码列表

    #### 接口依赖
    #### 边界异常说明
    #### 输入

    #### 前置检查

    #### 权限检查

    #### 逻辑处理

    #### 数据变更

    #### 事务处理
    #### 输出
        list

     **/
    order_response.ListExperienceCouponResponse listExperienceCoupon(1: order_request.ListExperienceCouponRequest request)

}