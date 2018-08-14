package com.github.dapeng.core;

/**
 * @author craneding
 * @date 15/9/10
 */
public enum SoaCode implements SoaBaseCodeInterface {
    // 客户端
    ClientUnKnown("Err-Core-400", "系统出错了!"),
    NoMatchedRouting("Err-Core-401", "没有可用路由"),
    NoMatchedVersion("Err-Core-403", "没有对应的服务版本"),
    NotFoundServer("Err-Core-404", "无可用的服务实例"),
    NoMatchedMethod("Err-Core-405", "没有对应的方法"),
    NotConnected("Err-Core-406", "连接失败"),
    ReqTimeOut("Err-Core-407", "请求超时"),
    ReqFieldNull("Err-Core-411" , "请求对象字段不允许为空"),
    RespFieldNull("Err-Core-412" , "响应对象字段不允许为空"),
    RespDecodeError("Err-Core-413", "响应通讯包解析出错"),

    // 服务端
    ServerUnKnown("Err-Core-500", "系统出错了!"),
    NoMatchedService("Err-Core-504", "没有对应的服务或者没有对应的服务版本"),
    ServerNoMatchedMethod("Err-Core-505", "没有对应的方法"),
    ServerReqTimeOut("Err-Core-506", "请求超时"),
    ReqBufferOverFlow("Err-Core-510", "请求过大"),
    ServerReqFieldNull("Err-Core-511", "请求对象字段不允许为空"),
    ServerRespFieldNull("Err-Core-512", "响应对象字段不允许为空"),
    ReqDecodeError("Err-Core-513", "请求通讯包解析出错"),
    ShmInitError("Err-Core-520", "限流模块初始化失败"),
    FreqLimited("Err-Core-521", "客户端已被限流"),
    FreqConfigError("Err-Core-522", "限流规则解析出错"),
    FreqControlError("Err-Core-523", "限流处理出错"),
    // 通用错误码
    StructFieldNull("Err-Core-600", "结构体字段不允许为空");
    private String code;
    private String msg;

    SoaCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return code + ":" + msg;
    }
}
