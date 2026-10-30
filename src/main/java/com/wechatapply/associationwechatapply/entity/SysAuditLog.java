package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志（SRS 表 5-5 sys_audit_log）：仅查询、不可修改删除（SRS FR-10-02）。
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 操作人（用户 id 或 ANONYMOUS） */
    private String operateBy;
    private String action;
    private String paramsBrief;
    private String ip;
    private LocalDateTime operateAt;
    /** SUCCESS / FAIL:原因 */
    private String result;
    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
