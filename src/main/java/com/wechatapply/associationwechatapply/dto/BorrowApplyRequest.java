package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅申请（SRS 用例 UC-07）。
 */
@Data
public class BorrowApplyRequest {

    @NotNull(message = "档案内部编号不能为空")
    private Long bizId;

    @NotBlank(message = "借阅事由不能为空")
    private String reason;

    @NotNull(message = "使用期限不能为空")
    private LocalDateTime dueAt;
}
