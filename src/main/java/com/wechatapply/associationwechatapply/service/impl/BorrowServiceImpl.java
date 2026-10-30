package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.dto.BorrowApplyRequest;
import com.wechatapply.associationwechatapply.dto.BorrowApproveRequest;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizBorrow;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.mapper.BizArchiveMapper;
import com.wechatapply.associationwechatapply.mapper.BizBorrowMapper;
import com.wechatapply.associationwechatapply.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    public static final int BS_PENDING = 0;
    public static final int BS_BORROWING = 1;
    public static final int BS_RETURNED = 2;
    public static final int BS_REJECTED = 4;

    private final BizBorrowMapper borrowMapper;
    private final BizArchiveMapper archiveMapper;

    @Override
    public IPage<BizBorrow> page(Page<BizBorrow> page, Integer status) {
        // 查询前先处理到期自动归还，保证状态及时
        autoReturnExpired();
        return borrowMapper.selectPage(page, Wrappers.<BizBorrow>lambdaQuery()
                .eq(status != null, BizBorrow::getBorrowStatus, status)
                // 协会管理员可见本会发起的借阅；主管部门/领导全局可见
                .eq(LoginContextHolder.getAssocCode() != null,
                        BizBorrow::getApplicantAssoc, LoginContextHolder.getAssocCode())
                .orderByDesc(BizBorrow::getBorrowId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizBorrow apply(BorrowApplyRequest req) {
        if (LoginContextHolder.getAssocCode() == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅商协会管理员可申请借阅");
        }
        BizArchive a = archiveMapper.selectOne(Wrappers.<BizArchive>lambdaQuery()
                .eq(BizArchive::getBizId, req.getBizId()));
        if (a == null || a.getStatus() != ArchiveServiceImpl.ST_ARCHIVED) {
            throw new BusinessException(ResultCode.BORROW_NOT_ALLOWED, "档案不存在或尚未归档");
        }
        // 同一档案重复申请：已有待审批/借阅中的记录则不再受理
        Long active = borrowMapper.selectCount(Wrappers.<BizBorrow>lambdaQuery()
                .eq(BizBorrow::getBizId, req.getBizId())
                .eq(BizBorrow::getApplicantId, LoginContextHolder.getUserId())
                .in(BizBorrow::getBorrowStatus, BS_PENDING, BS_BORROWING));
        if (active != null && active > 0) {
            throw new BusinessException(ResultCode.BORROW_NOT_ALLOWED, "该档案已在借阅流程中，请勿重复申请");
        }
        // 密级为内部的档案不对商协会开放借阅（SRS 4.2 规则 3）
        if (a.getSecurityLevel() != null && a.getSecurityLevel() == 2) {
            throw new BusinessException(ResultCode.BORROW_NOT_ALLOWED, "内部密级档案不开放借阅");
        }
        BizBorrow b = new BizBorrow();
        b.setBizId(req.getBizId());
        b.setArchiveId(a.getArchiveId());
        b.setApplicantId(LoginContextHolder.getUserId());
        b.setApplicantName(String.valueOf(LoginContextHolder.getUserId()));
        b.setApplicantAssoc(LoginContextHolder.getAssocCode());
        b.setReason(req.getReason());
        b.setDueAt(req.getDueAt());
        b.setBorrowStatus(BS_PENDING);
        borrowMapper.insert(b);
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(BorrowApproveRequest req) {
        BizBorrow b = borrowMapper.selectById(req.getBorrowId());
        if (b == null || b.getBorrowStatus() != BS_PENDING) {
            throw new BusinessException(ResultCode.BORROW_STATUS_INVALID);
        }
        b.setApproveBy(LoginContextHolder.getUserId());
        b.setApproveAt(LocalDateTime.now());
        b.setApproveOpinion(req.getOpinion());
        b.setApproveResult(req.isPass() ? 1 : 2);
        b.setBorrowStatus(req.isPass() ? BS_BORROWING : BS_REJECTED);
        if (req.isPass()) {
            b.setBorrowAt(LocalDateTime.now());
        }
        borrowMapper.updateById(b);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void giveBack(Long borrowId) {
        BizBorrow b = borrowMapper.selectById(borrowId);
        if (b == null || b.getBorrowStatus() != BS_BORROWING) {
            throw new BusinessException(ResultCode.BORROW_STATUS_INVALID);
        }
        // 行级隔离：协会只能归还自己发起的借阅，主管部门不受限
        String own = LoginContextHolder.getAssocCode();
        if (own != null && !own.equals(b.getApplicantAssoc())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能归还本会发起的借阅");
        }
        b.setReturnAt(LocalDateTime.now());
        b.setBorrowStatus(BS_RETURNED);
        // 归还时封存查阅留痕（SRS FR-07-04）
        b.setTraceLog((b.getTraceLog() == null ? "" : b.getTraceLog()) + "已登记归还；");
        borrowMapper.updateById(b);
    }

    /**
     * 到期自动归还（定时任务与查询前调用）：
     * 不再产生“逾期”状态，到达应还时间即视为已归还并封存查阅留痕。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoReturnExpired() {
        List<BizBorrow> expired = borrowMapper.selectList(Wrappers.<BizBorrow>lambdaQuery()
                .eq(BizBorrow::getBorrowStatus, BS_BORROWING)
                .lt(BizBorrow::getDueAt, LocalDateTime.now()));
        for (BizBorrow b : expired) {
            b.setBorrowStatus(BS_RETURNED);
            b.setReturnAt(b.getDueAt() == null ? LocalDateTime.now() : b.getDueAt());
            b.setTraceLog((b.getTraceLog() == null ? "" : b.getTraceLog()) + "系统到期自动归还；");
            borrowMapper.updateById(b);
        }
        return expired.size();
    }
}
