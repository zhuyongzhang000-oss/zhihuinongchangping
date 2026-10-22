package com.agritrace.security;

import com.agritrace.common.BusinessException;

/** 保存当前请求的登录用户，请求结束由拦截器清理 */
public final class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return user;
    }

    public static Long userId() {
        return get().id();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private UserContext() {
    }
}
