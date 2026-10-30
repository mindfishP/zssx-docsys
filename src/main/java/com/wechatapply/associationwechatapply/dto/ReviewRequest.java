package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核结论请求（SRS 用例 UC-06）。
 */
@Data
public class ReviewRequest {

    @NotNull(message = "档案内部编号不能为空")
    private Long bizId;

    /** true=通过并归档；false=退回 */
    private boolean pass;

    /** 退回时必填的审核意见 */
    private String opinion;
}
