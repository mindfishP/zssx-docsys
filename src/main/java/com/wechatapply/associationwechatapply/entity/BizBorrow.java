package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录（SRS 表 5-4 biz_borrow）：申请、审批、查阅、归还全流程留痕。
 */
@Data
@TableName("biz_borrow")
public class BizBorrow {

    @TableId(type = IdType.AUTO)
    private Long borrowId;

    private Long bizId;
    private String archiveId;

    private Long applicantId;
    private String applicantName;
    private String applicantAssoc;

    private String reason;

    /** 借出、应还与实际归还时间 */
    private LocalDateTime borrowAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnAt;

    private Long approveBy;
    private LocalDateTime approveAt;
    /** 审批结论：1 通过，2 拒绝 */
    private Integer approveResult;
    private String approveOpinion;

    /** 状态：0 待审批，1 借阅中，2 已归还，3 已逾期，4 已拒绝 */
    private Integer borrowStatus;

    /** 查阅行为留痕（JSON 数组，SRS FR-07-04） */
    private String traceLog;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
