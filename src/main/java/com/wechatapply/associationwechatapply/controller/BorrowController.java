package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.BorrowApplyRequest;
import com.wechatapply.associationwechatapply.dto.BorrowApproveRequest;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizBorrow;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.BizArchiveMapper;
import com.wechatapply.associationwechatapply.mapper.BizBorrowMapper;
import com.wechatapply.associationwechatapply.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 档案借阅接口（SRS FR-07 / UC-07）。
 */
@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final BizArchiveMapper archiveMapper;
    private final BizBorrowMapper borrowMapper;

    /** 借阅台账：协会见本会，主管部门与领导全局（数据范围在服务层过滤） */
    @GetMapping("/page")
    public R<IPage<BizBorrow>> page(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "20") long size,
                                    @RequestParam(required = false) Integer status) {
        return R.ok(borrowService.page(Page.of(page, size), status));
    }

    /**
     * 可借阅档案：已归档、密级为公开、且不属于本会；
     * 已在本会借阅流程中（待审批/借阅中）的档案不再重复列出。
     */
    @GetMapping("/available")
    public R<IPage<BizArchive>> available(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "20") long size,
                                          @RequestParam(required = false) String keyword) {
        String own = LoginContextHolder.getAssocCode();
        Long me = LoginContextHolder.getUserId();
        java.util.List<Long> active = borrowMapper.selectList(Wrappers.<BizBorrow>lambdaQuery()
                        .eq(BizBorrow::getApplicantId, me)
                        .in(BizBorrow::getBorrowStatus, 0, 1))
                .stream().map(BizBorrow::getBizId).toList();
        return R.ok(archiveMapper.selectPage(Page.of(page, size), Wrappers.<BizArchive>lambdaQuery()
                .eq(BizArchive::getStatus, 3)
                .eq(BizArchive::getSecurityLevel, 1)
                .ne(own != null, BizArchive::getAssocCode, own)
                .notIn(!active.isEmpty(), BizArchive::getBizId, active)
                .like(keyword != null && !keyword.isBlank(), BizArchive::getTitle, keyword)
                .orderByDesc(BizArchive::getBizId)));
    }

    /** 协会管理员发起借阅申请 */
    @PostMapping("/apply")
    @RequireRole(RoleCode.ASSOC)
    @AuditLog(action = "借阅申请")
    public R<BizBorrow> apply(@Valid @RequestBody BorrowApplyRequest req) {
        return R.ok(borrowService.apply(req));
    }

    /** 主管部门操作员审批 */
    @PostMapping("/approve")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "借阅审批")
    public R<Void> approve(@Valid @RequestBody BorrowApproveRequest req) {
        borrowService.approve(req);
        return R.ok();
    }

    /** 归还 */
    @PostMapping("/{borrowId}/return")
    @RequireRole({RoleCode.ASSOC, RoleCode.ADMIN})
    @AuditLog(action = "借阅归还")
    public R<Void> giveBack(@PathVariable Long borrowId) {
        borrowService.giveBack(borrowId);
        return R.ok();
    }
}
