package com.wechatapply.associationwechatapply.common.util;

import com.wechatapply.associationwechatapply.common.util.ActivityScoreCalculator.Metrics;
import com.wechatapply.associationwechatapply.common.util.ActivityScoreCalculator.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 活跃度加权评分（SRS 表 3-11）。
 */
class ActivityScoreCalculatorTest {

    @Test
    @DisplayName("满分场景：指标均达标且无退回，综合得分 100、示范型")
    void fullScore() {
        Result r = ActivityScoreCalculator.calculate(
                new Metrics(3, 6, 6, true, 10, 0));
        assertEquals(100.0, r.score());
        assertEquals(1, r.level());
        assertEquals(100.0, r.memberIndex());
        assertEquals(100.0, r.activityIndex());
        assertEquals(100.0, r.coverageRate());
        assertEquals(100.0, r.annualIndex());
        assertEquals(100.0, r.passRate());
    }

    @Test
    @DisplayName("空档案场景：只有默认通过率得分，判为休眠型")
    void emptyAssociation() {
        Result r = ActivityScoreCalculator.calculate(
                new Metrics(0, 0, 0, false, 0, 0));
        // 仅“审核一次通过率（无记录按 100）”贡献 10 分
        assertEquals(10.0, r.score());
        assertEquals(3, r.level());
        assertEquals("休眠型", ActivityScoreCalculator.levelText(r.level()));
    }

    @Test
    @DisplayName("加权求和：会员 1/3、活动 3/6、覆盖 3/6、年检达标、通过率 80%")
    void weightedSum() {
        Result r = ActivityScoreCalculator.calculate(
                new Metrics(1, 3, 3, true, 5, 1));
        // 33.3*0.25 + 50*0.25 + 50*0.2 + 100*0.2 + 80*0.1 = 8.33+12.5+10+20+8 = 58.8
        assertEquals(58.8, r.score());
        assertEquals(3, r.level());   // < 60 休眠
    }

    @Test
    @DisplayName("分档边界：80 分示范、60 分成长、59.9 分休眠")
    void levelBoundaries() {
        assertEquals(1, ActivityScoreCalculator.levelOf(80.0));
        assertEquals(1, ActivityScoreCalculator.levelOf(100.0));
        assertEquals(2, ActivityScoreCalculator.levelOf(60.0));
        assertEquals(2, ActivityScoreCalculator.levelOf(79.9));
        assertEquals(3, ActivityScoreCalculator.levelOf(59.9));
    }

    @Test
    @DisplayName("指标封顶 100：档案数超过目标值不会溢出")
    void indicesAreCapped() {
        assertEquals(100.0, ActivityScoreCalculator.memberIndex(99));
        assertEquals(100.0, ActivityScoreCalculator.activityIndex(99));
        assertEquals(100.0, ActivityScoreCalculator.coverageRate(6));
        assertTrue(ActivityScoreCalculator.passRate(10, 0) <= 100.0);
    }

    @Test
    @DisplayName("审核通过率：退回越多得分越低")
    void passRateDropsWithRejections() {
        assertEquals(100.0, ActivityScoreCalculator.passRate(0, 0));
        assertEquals(80.0, ActivityScoreCalculator.passRate(5, 1));
        assertEquals(50.0, ActivityScoreCalculator.passRate(4, 2));
        assertEquals(0.0, ActivityScoreCalculator.passRate(3, 3));
    }
}
