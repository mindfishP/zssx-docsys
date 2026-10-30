package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改姓名请求：账号用于登录，姓名用于展示，两者分开维护。
 */
@Data
public class UpdateNameRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名不超过 32 个字符")
    private String realName;
}
