package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 政策推送（SRS 表 5-5 biz_policy_push）。
 */
@Data
@TableName("biz_policy_push")
public class BizPolicyPush {

    @TableId(type = IdType.AUTO)
    private Long pushId;

    private String title;
    /** 富文本内容 */
    private String content;
    /** 附件相对路径（可空） */
    private String attachPath;

    /** 目标范围：ALL / INDUSTRY / LEVEL / LIST（SRS FR-09-02） */
    private String targetScope;
    /** 范围参数 JSON：行业列表 / 活跃度分档 / 协会名单 */
    private String targetValue;

    private String createdBy;
    private LocalDateTime publishedAt;

    /** 状态：0 草稿，1 已发布，2 已撤回 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
