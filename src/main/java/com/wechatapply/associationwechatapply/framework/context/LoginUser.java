package com.wechatapply.associationwechatapply.framework.context;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户上下文（由 JwtInterceptor 解析后写入 ThreadLocal）。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    /** 角色编码：ADMIN / LEADER / ASSOC */
    private String roleCode;
    /** 商协会管理员所属协会编号，其他角色为 null */
    private String assocCode;
}
