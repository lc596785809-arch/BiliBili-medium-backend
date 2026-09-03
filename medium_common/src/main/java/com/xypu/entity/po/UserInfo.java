package com.xypu.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_info")
public class UserInfo {

    @TableId(type = IdType.INPUT)
    private String userId;

    private String account;

    private String nickName;

    private String avatar;

    private String password;

    private String personIntroduction;

    private Date registerTime;

    private Date lastLoginTime;

    private String lastLoginIp;

    private Integer status;

    private String noticeInfo;

    private Integer permission;
}
