package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邀请码注册请求（SRS FR-01）：账号 + 口令 + 邀请码。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "请输入账号")
    @Size(min = 3, max = 32, message = "账号长度需为 3-32 位")
    private String username;

    @NotBlank(message = "请输入口令")
    @Size(min = 6, max = 32, message = "口令长度需为 6-32 位")
    private String password;

    @NotBlank(message = "请输入邀请码")
    private String inviteCode;
}
