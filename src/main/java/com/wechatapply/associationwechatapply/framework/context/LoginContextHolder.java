package com.wechatapply.associationwechatapply.framework.context;

import org.springframework.util.StringUtils;

/**
 * 登录用户 ThreadLocal 持有器。
 * 行级数据隔离（SRS 4.2）要求在数据访问层统一读取当前用户，禁止前端传入可信身份。
 */
public final class LoginContextHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginContextHolder() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String getRoleCode() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getRoleCode();
    }

    /** 商协会管理员返回其协会编号；主管部门角色返回 null（全局可见）。 */
    public static String getAssocCode() {
        LoginUser u = HOLDER.get();
        String code = u.getAssocCode();
        return StringUtils.hasText(code) ? code : null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
