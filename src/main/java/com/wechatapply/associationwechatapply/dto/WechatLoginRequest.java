package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小程序微信登录请求（SRS 7.2）：wx.login 取得的 code 由后端换取 openid。
 */
@Data
public class WechatLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;
}
