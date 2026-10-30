package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案条目（SRS 表 5-1 biz_archive）：采集、审核、归档的基本单位。
 */
@Data
@TableName("biz_archive")
public class BizArchive {

    /** 档案唯一标识 ZSA-…，草稿阶段为空，归档时生成并保持唯一（SRS 5.3） */
    private String archiveId;

    /** 内部自增主键，供外键关联与分页排序 */
    @TableId(type = IdType.AUTO)
    private Long bizId;

    private String assocCode;
    /** 分类编码（大类+中类） */
    private String categoryCode;
    /** 冗余大类码，便于按六大类统计 */
    private String majorCode;

    private String title;
    private String archiveYear;
    private String responsibility;

    /** 密级：1 公开，2 内部 */
    private Integer securityLevel;
    /** 保管期限：永久 / 定期30年 / 定期10年 */
    private String retention;

    /** 状态：0 草稿，1 待审核，2 审核退回，3 已归档 */
    private Integer status;

    /** 退回意见（最近一次，SRS FR-06） */
    private String rejectReason;

    private String submitBy;
    private LocalDateTime submitAt;
    private String archivedBy;
    private LocalDateTime archivedAt;

    /** 来源任务编号：专项补录产生的条目记录来源（SRS 3.6） */
    private String taskCode;

    /** 创建人用户 id：草稿状态仅创建人本人可见（SRS FR-04-05） */
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
