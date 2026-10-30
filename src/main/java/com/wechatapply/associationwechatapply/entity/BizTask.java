package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专项补录任务（SRS 3.6，biz_task）。
 */
@Data
@TableName("biz_task")
public class BizTask {

    /** 任务编号，如 T20260001 */
    @TableId(type = IdType.INPUT)
    private String taskCode;

    private String taskName;
    private String taskDesc;

    /** 涉及档案大类码，多个用逗号分隔；空表示不限 */
    private String majorCodes;

    /** 目标范围：ALL 全部，INDUSTRY 按行业，LIST 指定名单 */
    private String targetScope;
    /** scope=INDUSTRY 时的行业列表；scope=LIST 时的协会编号列表，JSON 数组 */
    private String targetValue;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String createdBy;

    /** 状态：1 进行中，2 已结束，0 已撤回 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
