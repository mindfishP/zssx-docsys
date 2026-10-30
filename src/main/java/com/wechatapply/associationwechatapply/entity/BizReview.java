package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录（SRS 表 5-5 biz_review）：审核节点与意见全程留痕。
 */
@Data
@TableName("biz_review")
public class BizReview {

    @TableId(type = IdType.AUTO)
    private Long reviewId;

    private Long bizId;
    private String archiveId;

    /** 审核节点：1 初审，2 复审（重要大类可启用复审，SRS 3.7） */
    private Integer nodeLevel;
    private Long reviewerId;
    private String reviewerName;

    /** 审核结果：1 通过，2 退回 */
    private Integer reviewResult;
    private String reviewOpinion;

    private LocalDateTime reviewAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
