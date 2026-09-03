package com.xypu.exception;

public enum ErrorCodeEnum {
    CODE_600(600, "请求参数错误"),
    CODE_601(601, "验证码错误或已过期"),
    CODE_602(602, "账号已存在"),
    CODE_603(603, "账号或密码错误"),
    CODE_604(604, "账号已禁用"),
    CODE_605(605, "无后台管理访问权限"),
    CODE_401(401, "未登录或会话已过期"),
    CODE_500(500, "服务器内部错误");

    private final Integer code;
    private final String msg;

    ErrorCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
