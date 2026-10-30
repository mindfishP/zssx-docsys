package com.wechatapply.associationwechatapply.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 发布政策推送请求（SRS FR-09-02）。
 * 推送范围支持：全部协会、勾选具体协会、按行业类别、按活跃度分档；
 * targets 为勾选的协会编号 / 行业类别 / 活跃度分档，数量不设上限。
 */
@Data
public class PushPublishRequest {

    @NotBlank(message = "推送标题不能为空")
    private String title;

    private String content;

    private String attachPath;

    /** ALL / LIST / INDUSTRY / LEVEL */
    @NotBlank(message = "推送范围不能为空")
    private String targetScope;

    /** 范围参数：LIST 为协会编号集合，INDUSTRY 为行业类别集合，LEVEL 为活跃度分档集合 */
    private List<String> targets;
}
