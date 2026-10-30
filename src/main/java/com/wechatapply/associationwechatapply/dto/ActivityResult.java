package com.wechatapply.associationwechatapply.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商协会活跃度评估结果（SRS FR-08 / 表 3-11），含各分项指标得分，便于看板展示与追溯。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResult {

    private String assocCode;
    private String assocName;
    private String industryType;

    /** 综合得分（百分制） */
    private double score;
    /** 1 示范型，2 成长型，3 休眠型 */
    private int level;
    private String levelText;

    /** 会员规模指数（25%） */
    private double memberIndex;
    /** 活动频次指数（25%） */
    private double activityIndex;
    /** 填报完整及时率（20%） */
    private double coverageRate;
    /** 年检达标（20%） */
    private double annualIndex;
    /** 审核一次通过率（10%） */
    private double passRate;
}
