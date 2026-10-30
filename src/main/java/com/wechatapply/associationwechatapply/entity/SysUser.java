package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号（SRS 表 5-5 sys_user）。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号（协会管理员建议使用协会编号） */
    private String username;

    /** BCrypt 口令哈希，接口层永不返回 */
    @JsonIgnore
    private String passwordHash;

    /** 真实姓名 */
    private String realName;

    /** 预置角色编码：ADMIN / LEADER / ASSOC */
    private String roleCode;

    /** 商协会管理员所属协会编号（assoc 编码），其他角色为空 */
    private String assocCode;

    private String phone;
    private String email;

    /** 微信 openid，小程序登录绑定后写入 */
    @JsonIgnore
    private String wxOpenid;

    /** 状态：1 正常，0 停用 */
    private Integer status;

    /** 连续登录失败次数（锁定策略用，SRS FR-01-01） */
    private Integer failCount;
    private LocalDateTime lockUntil;
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
