package com.wechatapply.associationwechatapply.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 批量导入回执（SRS FR-04-04）：总行数、成功数、失败数与逐行错误原因。
 */
@Data
public class ImportResult {

    private int total;
    private int success;
    private int failed;
    private List<RowError> errors = new ArrayList<>();

    public void addError(int row, String message) {
        this.failed++;
        this.errors.add(new RowError(row, message));
    }

    /** 行号从 1 开始（与 Excel 行号一致） */
    public record RowError(int row, String message) {
    }
}
