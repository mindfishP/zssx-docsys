package com.wechatapply.associationwechatapply.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 微信小程序开放接口封装（SRS 7.2）：
 * 用 wx.login 的 code 调用 jscode2session 换取 openid。
 * AppID/Secret 由 .env（或环境变量）注入，未配置时抛出明确提示而不是返回假 openid。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatGateway {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;

    @Value("${zssx.wechat.appid:}")
    private String appid;
    @Value("${zssx.wechat.secret:}")
    private String secret;

    /** 启动时打印配置状态，避免“点了一键登录才发现没配” */
    @PostConstruct
    void checkConfig() {
        if (!configured()) {
            log.warn("微信 AppID/Secret 未配置，微信一键登录将不可用；请在 .env 填写 ZSSX_WECHAT_APPID / ZSSX_WECHAT_SECRET");
        } else {
            log.info("微信登录已启用：appid={}，secret={}", appid, secret.isEmpty() ? "" : "******");
        }
    }

    private boolean configured() {
        return appid != null && !appid.isBlank() && !appid.startsWith("wx0000")
                && secret != null && !secret.isBlank();
    }

    /** code 换 openid；失败抛出 BusinessException（带微信返回的 errcode/errmsg） */
    public String getOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED, "缺少微信登录 code");
        }
        if (!configured()) {
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED,
                    "未配置微信小程序 AppID/Secret，请在 .env 中填写 ZSSX_WECHAT_APPID 与 ZSSX_WECHAT_SECRET");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + enc(appid)
                + "&secret=" + enc(secret)
                + "&js_code=" + enc(code)
                + "&grant_type=authorization_code";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode body = objectMapper.readTree(response.body());
            String openid = body.path("openid").asText("");
            if (openid.isBlank()) {
                int errcode = body.path("errcode").asInt(0);
                String errmsg = body.path("errmsg").asText("未知错误");
                log.warn("jscode2session 失败: errcode={}, errmsg={}", errcode, errmsg);
                throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED,
                        "微信登录失败（" + errcode + "）：" + errmsg);
            }
            return openid;
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED, "调用微信接口被中断");
        } catch (Exception e) {
            log.error("调用微信 jscode2session 异常", e);
            throw new BusinessException(ResultCode.WECHAT_LOGIN_FAILED, "调用微信接口失败：" + e.getMessage());
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
