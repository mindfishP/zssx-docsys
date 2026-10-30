package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.entity.SysAuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志查询接口（SRS FR-10-02）：仅主管部门操作员与主管领导，只读。
 */
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final SysAuditLogMapper auditLogMapper;

    @GetMapping("/page")
    @RequireRole({RoleCode.ADMIN, RoleCode.LEADER})
    public R<IPage<SysAuditLog>> page(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size,
                                      @RequestParam(required = false) String action) {
        return R.ok(auditLogMapper.selectPage(Page.of(page, size), Wrappers.<SysAuditLog>lambdaQuery()
                .like(action != null && !action.isBlank(), SysAuditLog::getAction, action)
                .orderByDesc(SysAuditLog::getLogId)));
    }
}
