package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.entity.BizTask;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.BizTaskMapper;
import com.wechatapply.associationwechatapply.service.TaskStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

/**
 * 专项补录任务接口（SRS FR-05 / UC-05）。
 */
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final BizTaskMapper taskMapper;
    private final TaskStatusService taskStatusService;

    /** 任务列表：协会角色仅见进行中任务与全局列表 */
    @GetMapping("/page")
    public R<IPage<BizTask>> page(@RequestParam(defaultValue = "1") long page,
                                  @RequestParam(defaultValue = "20") long size) {
        // 查询前先处理到期自动结束，保证状态及时
        taskStatusService.autoEndExpired();
        return R.ok(taskMapper.selectPage(Page.of(page, size), Wrappers.<BizTask>lambdaQuery()
                .orderByDesc(BizTask::getTaskCode)));
    }

    /** 任务详情：用于查看任务说明与补录要求 */
    @GetMapping("/{taskCode}")
    public R<BizTask> detail(@PathVariable String taskCode) {
        BizTask task = taskMapper.selectById(taskCode);
        if (task == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "任务不存在");
        }
        return R.ok(task);
    }

    /** 发起专项补录任务（仅主管部门操作员） */
    @PostMapping
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "发起专项补录")
    public R<BizTask> create(@RequestBody BizTask task) {
        task.setTaskCode("T" + java.time.LocalDate.now().getYear()
                + String.format("%04d", taskMapper.selectCount(null) + 1));
        task.setCreatedBy(String.valueOf(com.wechatapply.associationwechatapply.framework.context.LoginContextHolder.getUserId()));
        if (task.getStatus() == null) {
            task.setStatus(1);
        }
        taskMapper.insert(task);
        return R.ok(task);
    }

    /** 撤回未开始任务 */
    @PostMapping("/{taskCode}/withdraw")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "撤回补录任务")
    public R<Void> withdraw(@PathVariable String taskCode) {
        BizTask t = taskMapper.selectById(taskCode);
        if (t != null) {
            t.setStatus(0);
            taskMapper.updateById(t);
        }
        return R.ok();
    }

    /** 编号格式化占位（保留 DateTimeFormatter 引用供后续任务详情页使用） */
    @SuppressWarnings("unused")
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
}
