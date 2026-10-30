package com.wechatapply.associationwechatapply.dto;

import org.springframework.core.io.Resource;

/**
 * 附件下载/查阅结果：流式返回所需的资源、原始文件名与 MIME 类型。
 * inline=true 表示仅供在线查阅，不作为附件保存。
 */
public record DownloadFile(Resource resource, String fileName, String contentType, boolean inline) {

    public DownloadFile(Resource resource, String fileName, String contentType) {
        this(resource, fileName, contentType, false);
    }
}