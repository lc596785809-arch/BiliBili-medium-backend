package com.xypu.entity.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String account;
    private String password;
    private String nickName;
    private String checkCode;
    private String checkCodeKey;
}
