package com.xypu.context;

import com.xypu.entity.vo.UserInfoVO;

public class UserContext {

    private static final ThreadLocal<UserInfoVO> HOLDER = new ThreadLocal<>();

    public static void set(UserInfoVO user) {
        HOLDER.set(user);
    }

    public static UserInfoVO get() {
        return HOLDER.get();
    }

    public static void remove() {
        HOLDER.remove();
    }
}
