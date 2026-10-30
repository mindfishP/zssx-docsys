package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建邀请码请求（SRS FR-01）：预置注册后的角色与所属协会。
 */
@Data
public class CreateInviteRequest {

    /** ADMIN / LEADER / ASSOC */
    @NotBlank(message = "请选择角色")
    private String roleCode;

    /** 商协会管理员必填：注册后加入的协会编号 */
    private String assocCode;
}
