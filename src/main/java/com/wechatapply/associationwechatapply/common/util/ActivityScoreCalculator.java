package com.wechatapply.associationwechatapply.common.util;

/**
 * 商协会活跃度加权评分（SRS 表 3-11）。
 *
 * <p>指标与权重：会员规模指数 25%、活动频次指数 25%、填报完整及时率 20%、
 * 年检达标 20%、审核一次通过率 10%；综合得分 S = Σ(指标得分 × 权重)，百分制。
 * 分档：S ≥ 80 示范型（1），60 ≤ S &lt; 80 成长型（2），S &lt; 60 休眠型（3）。</p>
 *
 * <p>各项指标按档案数据归一化，口径集中在 {@link #memberIndex(long)} 等方法中，
 * 便于后续按主管部门实际考核要求调整。</p>
 */
public final class ActivityScoreCalculator {

    /** 会员类档案数达到该值即视为指标满分 */
    public static final int MEMBER_TARGET = 3;
    /** 活动类档案数达到该值即视为指标满分 */
    public static final int ACTIVITY_TARGET = 6;
    /** 六大类档案 */
    public static final int MAJOR_TOTAL = 6;

    public static final double W_MEMBER = 0.25;
    public static final double W_ACTIVITY = 0.25;
    public static final double W_COVERAGE = 0.20;
    public static final double W_ANNUAL = 0.20;
    public static final double W_PASS = 0.10;

    private ActivityScoreCalculator() {
    }

    /** 指标原始数据 */
    public record Metrics(long memberArchives, long activityArchives, int coveredMajors,
                          boolean annualCheckArchived, long reviewTotal, long reviewRejected) {
    }

    /** 指标得分与综合结果 */
    public record Result(double score, int level,
                         double memberIndex, double activityIndex,
                         double coverageRate, double annualIndex, double passRate) {
    }

    public static Result calculate(Metrics m) {
        double member = memberIndex(m.memberArchives());
        double activity = activityIndex(m.activityArchives());
        double coverage = coverageRate(m.coveredMajors());
        double annual = m.annualCheckArchived() ? 100.0 : 0.0;
        double pass = passRate(m.reviewTotal(), m.reviewRejected());
        double score = member * W_MEMBER + activity * W_ACTIVITY
                + coverage * W_COVERAGE + annual * W_ANNUAL + pass * W_PASS;
        score = round1(score);
        return new Result(score, levelOf(score), member, activity, coverage, annual, pass);
    }

    /** 会员规模指数：会员类归档数 / 目标数 × 100，封顶 100 */
    public static double memberIndex(long memberArchives) {
        return clamp(memberArchives * 100.0 / MEMBER_TARGET);
    }

    /** 活动频次指数：活动类归档数 / 目标数 × 100，封顶 100 */
    public static double activityIndex(long activityArchives) {
        return clamp(activityArchives * 100.0 / ACTIVITY_TARGET);
    }

    /** 填报完整及时率：有归档的大类数 / 6 × 100 */
    public static double coverageRate(int coveredMajors) {
        return clamp(coveredMajors * 100.0 / MAJOR_TOTAL);
    }

    /** 审核一次通过率：(1 - 退回次数 / 审核次数) × 100；无审核记录时按 100 处理 */
    public static double passRate(long reviewTotal, long reviewRejected) {
        if (reviewTotal <= 0) {
            return 100.0;
        }
        return clamp((1 - (double) reviewRejected / reviewTotal) * 100.0);
    }

    /** 分档：1 示范型，2 成长型，3 休眠型 */
    public static int levelOf(double score) {
        if (score >= 80) {
            return 1;
        }
        return score >= 60 ? 2 : 3;
    }

    public static String levelText(int level) {
        return switch (level) {
            case 1 -> "示范型";
            case 2 -> "成长型";
            default -> "休眠型";
        };
    }

    private static double clamp(double v) {
        if (v < 0) {
            return 0;
        }
        return Math.min(v, 100.0);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
