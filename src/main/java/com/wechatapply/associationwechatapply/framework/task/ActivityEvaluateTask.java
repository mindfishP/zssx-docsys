package com.wechatapply.associationwechatapply.framework.task;

import com.wechatapply.associationwechatapply.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活跃度定时评估（SRS FR-08）：默认每周一 02:00 重算全部协会得分与分档，
 * cron 可通过 zssx.activity.cron 配置覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEvaluateTask {

    private final ActivityService activityService;

    @Scheduled(cron = "${zssx.activity.cron:0 0 2 * * MON}")
    public void evaluateWeekly() {
        try {
            int count = activityService.evaluateAll().size();
            log.info("定时活跃度评估完成，共 {} 个协会", count);
        } catch (Exception e) {
            log.error("定时活跃度评估失败", e);
        }
    }
}
