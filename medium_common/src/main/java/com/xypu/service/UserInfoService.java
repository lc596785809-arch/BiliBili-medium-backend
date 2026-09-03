package com.xypu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xypu.entity.po.UserInfo;
import com.xypu.entity.vo.UserInfoVO;

public interface UserInfoService extends IService<UserInfo> {

    void register(String account, String password, String nickName, Integer permission);

    UserInfoVO login(String account, String password, String clientIp);

    void updateLoginInfo(String userId, String clientIp);

    UserInfo getByAccount(String account);
}
