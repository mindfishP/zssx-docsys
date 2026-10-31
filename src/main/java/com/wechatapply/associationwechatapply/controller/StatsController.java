package com.wechatapply.associationwechatapply.controller;

import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.common.util.SimpleXlsx;
import com.wechatapply.associationwechatapply.dto.ActivityResult;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.service.ActivityService;
import com.wechatapply.associationwechatapply.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    /** 导出协会活跃度报表（xlsx，仅主管部门/主管领导） */
    @GetMapping("/export")
    @RequireRole({RoleCode.ADMIN, RoleCode.LEADER})
    @AuditLog(action = "导出活跃度报表")
    public ResponseEntity<byte[]> export() throws IOException {
        List<ActivityResult> data = activityService.list();
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("协会编号", "协会名称", "行业类别", "综合得分", "分档",
                "会员规模", "活动频次", "完整率", "年检达标", "一次通过率"));
        for (ActivityResult r : data) {
            rows.add(List.of(
                    safe(r.getAssocCode()), safe(r.getAssocName()), safe(r.getIndustryType()),
                    String.format("%.1f", r.getScore()), safe(r.getLevelText()),
                    String.valueOf(Math.round(r.getMemberIndex())),
                    String.valueOf(Math.round(r.getActivityIndex())),
                    String.valueOf(Math.round(r.getCoverageRate())),
                    String.valueOf(Math.round(r.getAnnualIndex())),
                    String.valueOf(Math.round(r.getPassRate()))));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SimpleXlsx.write(out, "活跃度报表", rows);
        String filename = URLEncoder.encode("协会活跃度报表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(out.toByteArray());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /** 重新计算并写回全部协会的活跃度得分与分档（主管部门操作员） */
    @PostMapping("/activity/evaluate")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "重新计算活跃度")
    public R<List<ActivityResult>> evaluate() {
        return R.ok(activityService.evaluateAll());
    }
}
