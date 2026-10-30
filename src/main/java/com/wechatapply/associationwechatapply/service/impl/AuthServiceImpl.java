package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.JwtUtil;
import com.wechatapply.associationwechatapply.dto.LoginRequest;
import com.wechatapply.associationwechatapply.dto.RegisterRequest;
import com.wechatapply.associationwechatapply.dto.WechatLoginRequest;
import com.wechatapply.associationwechatapply.entity.SysInviteCode;
import com.wechatapply.associationwechatapply.entity.SysUser;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.SysInviteCodeMapper;
import com.wechatapply.associationwechatapply.mapper.SysUserMapper;
import com.wechatapply.associationwechatapply.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysInviteCodeMapper inviteMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${zssx.wechat.appid}")
    private String wxAppid;
    @Value("${zssx.wechat.secret}")
    private String wxSecret;

    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 15;

    @Override
    public Map<String, Object> login(LoginRequest req) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !encoder.matches(req.getPassword(), user.getPasswordHash())) {
            markFail(user);
            throw new BusinessException(ResultCode.USER_LOGIN_FAILED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.USER_LOCKED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        user.setFailCount(0);
        user.setLockUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.issue(user.getId(), user.getUsername(), user.getRoleCode(), user.getAssocCode());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("roleCode", user.getRoleCode());
        data.put("assocCode", user.getAssocCode());
        return data;
    }

    /**
     * 邀请码注册：邀请码由主管部门发放，绑定角色与协会，12 小时内有效。
     * 先原子核销邀请码，再创建账号，任一步失败整体回滚（避免邀请码被白白消耗）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest req) {
        String username = req.getUsername().trim();
        String code = req.getInviteCode().trim().toUpperCase();
        if (userMapper.countByUsername(username) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        SysInviteCode invite = inviteMapper.selectOne(Wrappers.<SysInviteCode>lambdaQuery()
                .eq(SysInviteCode::getCode, code));
        if (invite == null) {
            throw new BusinessException(ResultCode.INVITE_INVALID);
        }
        if (invite.getStatus() != null && invite.getStatus() == 1) {
            throw new BusinessException(ResultCode.INVITE_USED);
        }
        if (invite.getExpiresAt() == null || invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.INVITE_EXPIRED);
        }
        // 带条件更新实现原子核销：并发注册时只有一个请求能占用成功
        int claimed = inviteMapper.update(null, Wrappers.<SysInviteCode>lambdaUpdate()
                .eq(SysInviteCode::getInviteId, invite.getInviteId())
                .eq(SysInviteCode::getStatus, 0)
                .set(SysInviteCode::getStatus, 1)
                .set(SysInviteCode::getUsedAt, LocalDateTime.now()));
        if (claimed == 0) {
            throw new BusinessException(ResultCode.INVITE_USED);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(req.getPassword()));
        // 姓名与账号分开维护：注册时留空，登录后可在“账号管理”里填写真实姓名
        user.setRealName(null);
        user.setRoleCode(invite.getRoleCode());
        user.setAssocCode(invite.getAssocCode());
        user.setStatus(1);
        user.setFailCount(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 用户名唯一索引兜底（并发注册同名账号）
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        inviteMapper.update(null, Wrappers.<SysInviteCode>lambdaUpdate()
                .eq(SysInviteCode::getInviteId, invite.getInviteId())
                .set(SysInviteCode::getUsedBy, user.getId()));
    }

    /**
     * 小程序登录：正式实现需调用 https://api.weixin.qq.com/sns/jscode2session。
     * 框架阶段以 dev-code 规则模拟（code 即 openid），保证联调链路先跑通；
     * 接入真实 AppID 后仅需替换 fetchOpenid() 内部实现。
     */
    @Override
    public Map<String, Object> wechatLogin(WechatLoginRequest req) {
        String openid = fetchOpenid(req.getCode());
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getWxOpenid, openid));
        if (user == null) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED, "该微信尚未绑定系统账号，请先在 Web 端绑定");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        String token = jwtUtil.issue(user.getId(), user.getUsername(), user.getRoleCode(), user.getAssocCode());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("roleCode", user.getRoleCode());
        data.put("assocCode", user.getAssocCode());
        return data;
    }

    @Override
    public LoginUser parseToken(String token) {
        var claims = jwtUtil.parse(token);
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class),
                claims.get("assoc", String.class));
    }

    private void markFail(SysUser user) {
        if (user == null) {
            return;
        }
        int fails = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
        user.setFailCount(fails);
        if (fails >= MAX_FAIL) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setFailCount(0);
        }
        userMapper.updateById(user);
    }

    /** TODO: 替换为真实 jscode2session HTTP 调用。 */
    private String fetchOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED);
        }
        // 框架阶段：code 直接作为 openid，便于本地联调
        return "mock-openid-" + code;
    }
}
