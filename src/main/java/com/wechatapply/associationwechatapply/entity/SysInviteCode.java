package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 注册邀请码（SRS FR-01）：由主管部门操作员发放，绑定角色与协会，12 小时内有效。
 */
@Data
@TableName("sys_invite_code")
public class SysInviteCode {

    @TableId(type = IdType.AUTO)
    private Long inviteId;

    /** 邀请码（大写字母与数字，8 位），全局唯一 */
    private String code;

    /** 使用该邀请码注册后的角色：ADMIN / LEADER / ASSOC */
    private String roleCode;

    /** 使用该邀请码注册后加入的协会编号（ASSOC 角色必填） */
    private String assocCode;

    /** 状态：0 未使用，1 已使用 */
    private Integer status;

    /** 过期时间：创建后 12 小时 */
    private LocalDateTime expiresAt;

    /** 使用人用户 id */
    private Long usedBy;
    private LocalDateTime usedAt;

    /** 发放人用户 id */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
