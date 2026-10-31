package com.wechatapply.associationwechatapply.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.util.JwtUtil;
import com.wechatapply.associationwechatapply.dto.WechatLoginRequest;
import com.wechatapply.associationwechatapply.entity.SysUser;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;
import com.wechatapply.associationwechatapply.mapper.SysInviteCodeMapper;
import com.wechatapply.associationwechatapply.mapper.SysUserMapper;
import com.wechatapply.associationwechatapply.service.impl.AuthServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信绑定 / 解绑 / 一键登录（SRS FR-01 / 7.2）：
 * 微信接口用 WechatGateway 打桩，只校验业务规则。
 */
@ExtendWith(MockitoExtension.class)
class WechatBindTest {

    @Mock private SysUserMapper userMapper;
    @Mock private SysInviteCodeMapper inviteMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private WechatGateway wechatGateway;

    private AuthServiceImpl service;

    /** LambdaUpdateWrapper 需要 MyBatis-Plus 的表元数据缓存，单测里手动注册一次 */
    static {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysUser.class);
    }

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userMapper, inviteMapper, jwtUtil, wechatGateway);
        LoginContextHolder.set(new LoginUser(3L, "A001", "ASSOC", "A001"));
    }

    @AfterEach
    void tearDown() {
        LoginContextHolder.clear();
    }

    private SysUser user(Long id, String openid) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername("A001");
        u.setRoleCode("ASSOC");
        u.setAssocCode("A001");
        u.setStatus(1);
        u.setWxOpenid(openid);
        return u;
    }

    @Test
    @DisplayName("绑定成功：openid 写入当前账号")
    void bindSuccess() {
        SysUser me = user(3L, null);
        when(userMapper.selectById(3L)).thenReturn(me);
        when(wechatGateway.getOpenid("code-1")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(null);

        service.bindWechat("code-1");

        assertEquals("openid-1", me.getWxOpenid());
        verify(userMapper).updateById(me);
    }

    @Test
    @DisplayName("一个微信只能绑一个账号：命中他人时拒绝")
    void bindRejectsOtherAccount() {
        SysUser me = user(3L, null);
        SysUser other = user(9L, "openid-1");
        other.setUsername("B002");
        when(userMapper.selectById(3L)).thenReturn(me);
        when(wechatGateway.getOpenid("code-1")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(other);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.bindWechat("code-1"));
        assertTrue(ex.getMessage().contains("已绑定其他账号"));
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("重复绑定同一微信：幂等成功")
    void bindSameWechatTwice() {
        SysUser me = user(3L, "openid-1");
        when(userMapper.selectById(3L)).thenReturn(me);
        when(wechatGateway.getOpenid("code-1")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(me);

        service.bindWechat("code-1");

        assertEquals("openid-1", me.getWxOpenid());
        verify(userMapper).updateById(me);
    }

    @Test
    @DisplayName("解绑：显式清空 openid")
    void unbindClearsOpenid() {
        service.unbindWechat();
        verify(userMapper).update(any(), any());
    }

    @Test
    @DisplayName("绑定状态：有 openid 为已绑定，空值为未绑定")
    void wechatBoundStatus() {
        when(userMapper.selectById(3L)).thenReturn(user(3L, "openid-1"));
        assertTrue(service.wechatBound());

        when(userMapper.selectById(3L)).thenReturn(user(3L, null));
        assertFalse(service.wechatBound());
    }

    @Test
    @DisplayName("微信一键登录：已绑定账号签发 token")
    void wechatLoginIssuesToken() {
        SysUser bound = user(3L, "openid-1");
        when(wechatGateway.getOpenid("code-1")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(bound);
        when(jwtUtil.issue(anyLong(), any(), any(), any())).thenReturn("token-x");

        WechatLoginRequest req = new WechatLoginRequest();
        req.setCode("code-1");
        Map<String, Object> data = service.wechatLogin(req);

        assertEquals("token-x", data.get("token"));
        assertEquals("A001", data.get("assocCode"));
    }

    @Test
    @DisplayName("微信一键登录：未绑定提示先去“我的”绑定")
    void wechatLoginUnbound() {
        when(wechatGateway.getOpenid("code-1")).thenReturn("openid-x");
        when(userMapper.selectOne(any())).thenReturn(null);

        WechatLoginRequest req = new WechatLoginRequest();
        req.setCode("code-1");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.wechatLogin(req));
        assertTrue(ex.getMessage().contains("绑定"));
    }

    @Test
    @DisplayName("账号停用后不能用微信登录")
    void wechatLoginDisabledUser() {
        SysUser disabled = user(3L, "openid-1");
        disabled.setStatus(0);
        when(wechatGateway.getOpenid("code-1")).thenReturn("openid-1");
        when(userMapper.selectOne(any())).thenReturn(disabled);

        WechatLoginRequest req = new WechatLoginRequest();
        req.setCode("code-1");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.wechatLogin(req));
        assertEquals("账号已停用", ex.getMessage());
    }
}
