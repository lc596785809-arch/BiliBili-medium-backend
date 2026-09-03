package com.xypu.entity.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String account;
    private String password;
    private String checkCode;
    private String checkCodeKey;
}
