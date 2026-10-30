package com.wechatapply.associationwechatapply.controller;

import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.LoginRequest;
import com.wechatapply.associationwechatapply.dto.RegisterRequest;
import com.wechatapply.associationwechatapply.dto.WechatLoginRequest;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;
import com.wechatapply.associationwechatapply.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口（SRS FR-01）。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    /** 邀请码注册：账号口令 + 主管部门发放的邀请码 */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return R.ok();
    }

    @PostMapping("/wechat-login")
    public R<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginRequest req) {
        return R.ok(authService.wechatLogin(req));
    }

    /** 当前登录用户信息（需携带 token） */
    @GetMapping("/me")
    public R<LoginUser> me() {
        return R.ok(LoginContextHolder.get());
    }
}
