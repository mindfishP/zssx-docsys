package com.wechatapply.associationwechatapply.framework.interceptor;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.JwtUtil;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 认证 + 角色鉴权拦截器。
 * 白名单路径见 WebMvcConfig；携带 @RequireRole 的接口按注解校验角色。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parse(token);
        LoginUser user = new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class),
                claims.get("assoc", String.class)
        );
        LoginContextHolder.set(user);

        RequireRole rr = hm.getMethodAnnotation(RequireRole.class);
        if (rr == null) {
            rr = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (rr != null && rr.value().length > 0
                && Arrays.stream(rr.value()).noneMatch(r -> r.equals(user.getRoleCode()))) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContextHolder.clear();
    }
}
