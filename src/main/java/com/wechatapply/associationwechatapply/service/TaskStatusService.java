package com.wechatapply.associationwechatapply.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.entity.BizTask;
import com.wechatapply.associationwechatapply.mapper.BizTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专项补录任务状态维护（SRS FR-05）：
 * 到达截止时间的“进行中”任务自动置为“已结束”，不再需要人工处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private static final int ST_RUNNING = 1;
    private static final int ST_ENDED = 2;

    private final BizTaskMapper taskMapper;

    /** 到期自动结束，返回处理条数 */
    @Transactional(rollbackFor = Exception.class)
    public int autoEndExpired() {
        List<BizTask> expired = taskMapper.selectList(Wrappers.<BizTask>lambdaQuery()
                .eq(BizTask::getStatus, ST_RUNNING)
                .lt(BizTask::getEndTime, LocalDateTime.now()));
        for (BizTask task : expired) {
            task.setStatus(ST_ENDED);
            taskMapper.updateById(task);
        }
        if (!expired.isEmpty()) {
            log.info("专项补录任务到期自动结束 {} 条", expired.size());
        }
        return expired.size();
    }
}
