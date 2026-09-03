package com.xypu.enums;

public enum StatusEnum {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final Integer code;
    private final String desc;

    StatusEnum(Integer code, String desc) {
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
