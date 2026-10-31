package com.wechatapply.associationwechatapply.framework.interceptor;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录/注册接口限流（SRS FR-01 安全要求）：同一来源 60 秒内最多 30 次，防撞库与批量注册。
 * 账号维度还有“连续 5 次失败锁定 15 分钟”，两层互补。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 30;
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_KEYS = 5000;

    private final Map<String, Deque<Long>> records = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long now = System.currentTimeMillis();
        String ip = clientIp(request);
        Deque<Long> hits = records.computeIfAbsent(ip, key -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MS) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_REQUESTS) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
            }
            hits.addLast(now);
        }
        if (records.size() > MAX_KEYS) {
            evictExpired(now);
        }
        return true;
    }

    /** 仅当直连方是本机（如 Nginx 反代）时才信任 X-Forwarded-For，避免伪造头绕过限流 */
    private String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (isLoopback(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String first = forwarded.split(",")[0].trim();
                if (!first.isEmpty() && first.length() <= 45) {
                    return first;
                }
            }
        }
        return remote == null ? "unknown" : remote;
    }

    private boolean isLoopback(String ip) {
        return ip != null && (ip.startsWith("127.") || "::1".equals(ip) || "0:0:0:0:0:0:1".equals(ip));
    }

    private void evictExpired(long now) {
        records.entrySet().removeIf(entry -> {
            Deque<Long> hits = entry.getValue();
            synchronized (hits) {
                return hits.isEmpty() || now - hits.peekLast() > WINDOW_MS;
            }
        });
    }
}