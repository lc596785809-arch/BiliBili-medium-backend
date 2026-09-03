package com.xypu.enums;

public enum PermissionEnum {
    USER(1, "普通用户"),
    VIP(2, "VIP用户"),
    ADMIN(3, "管理员用户");

    private final Integer code;
    private final String desc;

    PermissionEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
