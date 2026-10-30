package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 借阅审批结论（SRS 用例 UC-07）。
 */
@Data
public class BorrowApproveRequest {

    @NotNull(message = "借阅记录编号不能为空")
    private Long borrowId;

    /** true=通过；false=拒绝 */
    private boolean pass;

    private String opinion;
}
