package com.wechatapply.associationwechatapply.framework.task;

import com.wechatapply.associationwechatapply.service.BorrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 借阅到期自动归还（SRS FR-07）：到达应还时间即自动登记归还，
 * 系统不再保留“逾期”状态。默认每天 01:00 执行，cron 可通过 zssx.borrow.return-cron 覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowAutoReturnTask {

    private final BorrowService borrowService;

    @Scheduled(cron = "${zssx.borrow.return-cron:0 0 1 * * *}")
    public void autoReturnDaily() {
        try {
            int count = borrowService.autoReturnExpired();
            if (count > 0) {
                log.info("借阅到期自动归还完成，共 {} 条", count);
            }
        } catch (Exception e) {
            log.error("借阅到期自动归还失败", e);
        }
    }
}