package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 档案填报/提交请求（SRS 用例 UC-04）。
 */
@Data
public class ArchiveSubmitRequest {

    /** 分类编码，如 B03 */
    @NotBlank(message = "档案分类不能为空")
    private String categoryCode;

    @NotBlank(message = "题名不能为空")
    private String title;

    /** 档案年度，如 2026 */
    @NotBlank(message = "档案年度不能为空")
    private String archiveYear;

    /** 责任者 */
    private String responsibility;

    /** 密级：1 公开，2 内部；空则取分类默认值 */
    private Integer securityLevel;

    /** 保管期限；空则取分类默认值 */
    private String retention;

    /** 已上传的附件 id 列表 */
    private List<Long> attachIds;

    /** true=提交送审；false=仅存草稿 */
    private boolean submit;
}
