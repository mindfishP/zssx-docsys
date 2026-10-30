package com.wechatapply.associationwechatapply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.dto.BorrowApplyRequest;
import com.wechatapply.associationwechatapply.dto.BorrowApproveRequest;
import com.wechatapply.associationwechatapply.entity.BizBorrow;

/**
 * 档案借阅服务（SRS FR-07，UC-07）。
 */
public interface BorrowService {

    IPage<BizBorrow> page(Page<BizBorrow> page, Integer status);

    /** 协会管理员发起借阅申请 */
    BizBorrow apply(BorrowApplyRequest req);

    /** 主管部门操作员审批：通过生成限时查阅授权 / 拒绝 */
    void approve(BorrowApproveRequest req);

    /** 归还：结束借阅并锁定留痕 */
    void giveBack(Long borrowId);

    /** 到期自动归还（定时任务/查询前调用，不再保留逾期状态） */
    int autoReturnExpired();
}
