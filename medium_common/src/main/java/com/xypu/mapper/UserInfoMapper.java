package com.xypu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xypu.entity.po.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    void updateLoginInfo(@Param("userId") String userId,
                         @Param("lastLoginTime") java.util.Date lastLoginTime,
                         @Param("lastLoginIp") String lastLoginIp);
}
