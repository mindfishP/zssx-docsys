package com.wechatapply.associationwechatapply.framework.task;

import com.wechatapply.associationwechatapply.service.TaskStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 专项补录到期自动结束（SRS FR-05）：默认每天 01:05 扫描，
 * 进行中且已过截止时间的任务自动置为“已结束”，cron 可通过 zssx.task.end-cron 覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAutoEndTask {

    private final TaskStatusService taskStatusService;

    @Scheduled(cron = "${zssx.task.end-cron:0 5 1 * * *}")
    public void autoEndDaily() {
        try {
            taskStatusService.autoEndExpired();
        } catch (Exception e) {
            log.error("专项补录任务到期结束失败", e);
        }
    }
}