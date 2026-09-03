package com.xypu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xypu.entity.po.UserInfo;
import com.xypu.entity.vo.UserInfoVO;
import com.xypu.enums.StatusEnum;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.mapper.UserInfoMapper;
import com.xypu.service.UserInfoService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public void register(String account, String password, String nickName, Integer permission) {
        // 账号唯一性校验，已存在则抛出业务异常
        if (getByAccount(account) != null) {
            throw new BusinessException(ErrorCodeEnum.CODE_602);
        }
        UserInfo user = new UserInfo();
        // 生成 10 位纯数字用户 ID
        user.setUserId(RandomStringUtils.randomNumeric(10));
        user.setAccount(account);
        user.setNickName(nickName);
        // BCrypt 加密密码后存储，禁止明文入库
        user.setPassword(passwordEncoder.encode(password));
        user.setPermission(permission);
        user.setStatus(StatusEnum.NORMAL.getCode());
        user.setRegisterTime(new Date());
        save(user);
    }

    @Override
    public UserInfoVO login(String account, String password, String clientIp) {
        // 账号不存在时与密码错误返回同一错误码，防止账号枚举攻击
        UserInfo user = getByAccount(account);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.CODE_603);
        }
        // 账号被禁用，直接拒绝登录
        if (StatusEnum.DISABLED.getCode().equals(user.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.CODE_604);
        }
        // BCrypt 比对，matches 内部处理盐值，禁止直接比较明文
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.CODE_603);
        }
        // 登录成功，更新最后登录时间和 IP
        Date now = new Date();
        userInfoMapper.updateLoginInfo(user.getUserId(), now, clientIp);
        user.setLastLoginTime(now);
        user.setLastLoginIp(clientIp);

        // 转换为 VO 返回，VO 中不含 password 字段
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateLoginInfo(String userId, String clientIp) {
        // 仅更新登录信息，用于自动登录场景（无需重新校验密码）
        userInfoMapper.updateLoginInfo(userId, new Date(), clientIp);
    }

    @Override
    public UserInfo getByAccount(String account) {
        return getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getAccount, account));
    }
}
