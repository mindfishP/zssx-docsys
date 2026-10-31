package com.wechatapply.associationwechatapply.service;

import com.wechatapply.associationwechatapply.dto.LoginRequest;
import com.wechatapply.associationwechatapply.dto.RegisterRequest;
import com.wechatapply.associationwechatapply.dto.WechatLoginRequest;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;

import java.util.Map;

/**
 * 认证服务（SRS FR-01）。
 */
public interface AuthService {

    /** 账号口令登录：校验口令、失败锁定策略，返回 token 与用户信息 */
    Map<String, Object> login(LoginRequest req);

    /** 邀请码注册：校验邀请码（角色/协会/12 小时有效期）后创建账号 */
    void register(RegisterRequest req);

    /** 小程序微信登录：code 换 openid，绑定账号后签发 token（SRS 7.2） */
    Map<String, Object> wechatLogin(WechatLoginRequest req);

    /** 绑定微信：当前登录账号 + wx.login 的 code → 写入 openid（一个微信仅能绑一个账号） */
    void bindWechat(String code);

    /** 解绑微信：清空当前账号的 openid */
    void unbindWechat();

    /** 当前账号是否已绑定微信 */
    boolean wechatBound();

    /** 解析 token 重建登录上下文（供拦截器之外的测试场景使用） */
    LoginUser parseToken(String token);
}
