package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.ChangePasswordRequest;
import com.wechatapply.associationwechatapply.dto.UpdateNameRequest;
import com.wechatapply.associationwechatapply.entity.SysUser;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账号管理接口（SRS FR-01）：
 * 所有角色均可修改本人密码；账号列表与删除仅主管部门操作员可用。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 修改本人密码 */
    @PutMapping("/password")
    @AuditLog(action = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(req);
        return R.ok();
    }

    /** 修改本人姓名（账号用于登录，姓名用于展示） */
    @PutMapping("/profile/name")
    @AuditLog(action = "修改姓名")
    public R<Void> updateMyName(@Valid @RequestBody UpdateNameRequest req) {
        userService.updateMyName(req.getRealName());
        return R.ok();
    }

    /** 主管部门修改指定账号姓名 */
    @PutMapping("/{id}/name")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "修改账号姓名")
    public R<Void> updateName(@PathVariable Long id, @Valid @RequestBody UpdateNameRequest req) {
        userService.updateName(id, req.getRealName());
        return R.ok();
    }

    /** 账号列表（仅主管部门操作员） */
    @GetMapping("/page")
    @RequireRole(RoleCode.ADMIN)
    public R<IPage<SysUser>> page(@RequestParam(defaultValue = "1") long page,
                                  @RequestParam(defaultValue = "20") long size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String roleCode) {
        return R.ok(userService.page(Page.of(page, size), keyword, roleCode));
    }

    /** 删除账号（仅主管部门操作员，逻辑删除并留痕） */
    @DeleteMapping("/{id}")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "删除账号")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }
}
