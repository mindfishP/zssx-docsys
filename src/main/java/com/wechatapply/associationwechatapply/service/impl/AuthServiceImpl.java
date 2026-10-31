package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.JwtUtil;
import com.wechatapply.associationwechatapply.common.util.PasswordPolicy;
import com.wechatapply.associationwechatapply.dto.LoginRequest;
import com.wechatapply.associationwechatapply.dto.RegisterRequest;
import com.wechatapply.associationwechatapply.dto.WechatLoginRequest;
import com.wechatapply.associationwechatapply.entity.SysInviteCode;
import com.wechatapply.associationwechatapply.entity.SysUser;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.SysInviteCodeMapper;
import com.wechatapply.associationwechatapply.mapper.SysUserMapper;
import com.wechatapply.associationwechatapply.service.AuthService;
import com.wechatapply.associationwechatapply.service.WechatGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final WechatGateway wechatGateway;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

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

        PasswordPolicy.check(req.getPassword());
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
     * 小程序微信登录：code 经 jscode2session 换取 openid，匹配已绑定的账号后签发 token。
     * 未绑定、账号停用、微信参数未配置都会给出明确提示。
     */
    @Override
    public Map<String, Object> wechatLogin(WechatLoginRequest req) {
        String openid = wechatGateway.getOpenid(req.getCode());
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getWxOpenid, openid));
        if (user == null) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED,
                    "该微信尚未绑定系统账号，请先用账号密码登录，在“我的”中绑定微信");
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

    /**
     * 绑定微信：当前登录账号 + wx.login 的 code → 换取 openid 后写入 sys_user.wx_openid。
     * 一个微信只能绑定一个账号；重复绑定同一微信视为幂等成功。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindWechat(String code) {
        Long userId = LoginContextHolder.getUserId();
        SysUser current = userId == null ? null : userMapper.selectById(userId);
        if (current == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String openid = wechatGateway.getOpenid(code);
        SysUser other = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getWxOpenid, openid));
        if (other != null && !other.getId().equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该微信已绑定其他账号（" + maskUsername(other.getUsername()) + "），请先解绑");
        }
        current.setWxOpenid(openid);
        try {
            userMapper.updateById(current);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该微信已被其他账号绑定");
        }
    }

    /** 解绑微信：清空当前账号的 openid，之后不能用微信一键登录 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindWechat() {
        Long userId = LoginContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        // updateById 会忽略 null 字段，这里必须用 set 显式置空
        userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(SysUser::getWxOpenid, null));
    }

    /** 当前账号是否已绑定微信 */
    @Override
    public boolean wechatBound() {
        Long userId = LoginContextHolder.getUserId();
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        return user != null && user.getWxOpenid() != null && !user.getWxOpenid().isBlank();
    }

    private String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "未知账号";
        }
        if (username.length() <= 2) {
            return username.charAt(0) + "*";
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }
}
