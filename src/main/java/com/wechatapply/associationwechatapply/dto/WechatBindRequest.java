package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 绑定微信请求：小程序 wx.login() 拿到的一次性 code。
 */
@Data
public class WechatBindRequest {

    @NotBlank(message = "微信登录 code 不能为空")
    private String code;
}
