package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商协会主档（SRS 表 5-2 biz_association，一协一档主线）。
 */
@Data
@TableName("biz_association")
public class BizAssociation {

    /** 协会编号（业务主键），如 A001 */
    @TableId(type = IdType.INPUT)
    private String assocCode;

    private String assocName;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 行业类别：灯饰/五金/家电/其他等 */
    private String industryType;

    private String registerOrg;
    private LocalDate registerDate;
    private String legalPerson;
    private String contactPhone;

    /** 活跃度分档：1 示范，2 成长，3 休眠（评估任务写入，SRS 3.9） */
    private Integer activityLevel;
    /** 最近一次活跃度综合得分（百分制） */
    private java.math.BigDecimal activityScore;

    /** 状态：1 正常，0 注销，2 异常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
