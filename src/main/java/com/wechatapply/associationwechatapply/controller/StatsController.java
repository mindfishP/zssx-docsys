package com.wechatapply.associationwechatapply.controller;

import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.ActivityResult;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.service.ActivityService;
import com.wechatapply.associationwechatapply.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计看板接口（SRS FR-08）。数据范围：主管部门/领导全局，协会仅本会。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final ActivityService activityService;

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(statsService.dashboard());
    }

    /** 活跃度指标明细：按 SRS 表 3-11 实时计算（不落库） */
    @GetMapping("/activity")
    public R<List<ActivityResult>> activity() {
        return R.ok(activityService.list());
    }

    /** 重新计算并写回全部协会的活跃度得分与分档（主管部门操作员） */
    @PostMapping("/activity/evaluate")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "重新计算活跃度")
    public R<List<ActivityResult>> evaluate() {
        return R.ok(activityService.evaluateAll());
    }
}
