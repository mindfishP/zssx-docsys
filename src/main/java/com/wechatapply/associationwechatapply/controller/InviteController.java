package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.CreateInviteRequest;
import com.wechatapply.associationwechatapply.entity.SysInviteCode;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.service.InviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 注册邀请码接口（SRS FR-01）：仅主管部门操作员可发放与查看。
 */
@RestController
@RequestMapping("/api/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    /** 创建邀请码：指定注册后的角色与协会，有效期 12 小时 */
    @PostMapping
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "创建邀请码")
    public R<SysInviteCode> create(@Valid @RequestBody CreateInviteRequest req) {
        return R.ok(inviteService.create(req));
    }

    /** 邀请码列表 */
    @GetMapping("/page")
    @RequireRole(RoleCode.ADMIN)
    public R<IPage<SysInviteCode>> page(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "20") long size,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(inviteService.page(Page.of(page, size), status));
    }
}
