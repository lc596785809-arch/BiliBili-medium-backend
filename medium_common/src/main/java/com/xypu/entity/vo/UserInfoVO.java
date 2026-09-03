package com.xypu.entity.vo;

import lombok.Data;

import java.util.Date;

@Data
public class UserInfoVO {

    private String userId;

    private String account;

    private String nickName;

    private String avatar;

    private String personIntroduction;

    private Date registerTime;

    private Date lastLoginTime;

    private String lastLoginIp;

    private Integer status;

    private String noticeInfo;

    private Integer permission;
}
