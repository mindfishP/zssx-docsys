package com.wechatapply.associationwechatapply.service;

import com.wechatapply.associationwechatapply.dto.DownloadFile;
import com.wechatapply.associationwechatapply.entity.BizAttachment;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务（SRS 5.4）：按 年度/大类/协会 目录分档 + SHA-256 固证。
 */
public interface FileStorageService {

    /**
     * 上传附件：校验格式与大小，落盘并登记哈希。
     *
     * @param bizId      所属档案条目内部编号（草稿阶段可为空，提交时回填）
     * @param majorCode  大类码（目录分档用，可空则归入 TEMP）
     * @param assocCode  协会编号
     */
    BizAttachment upload(Long bizId, String majorCode, String assocCode, MultipartFile file);

    /** 校验并回填哈希：归档时复核附件完整性（SRS 5.4） */
    boolean verifyHash(BizAttachment att);

    /** 生成时效访问签名 URL（框架阶段返回带令牌的下载接口地址） */
    String signedUrl(Long attachId);

    /**
     * 校验签名与权限后取得附件文件，供接口流式返回（SRS FR-10-03）。
     * 校验项：签名有效期、签名与当前用户绑定、协会行级隔离、文件存在且未越出存储根目录。
     */
    DownloadFile openForDownload(Long attachId, long expires, String token);

    /** 借阅查阅用签名地址：仅用于在线查看，不能作为附件另存 */
    String signedViewUrl(Long attachId);

    /** 借阅查阅：本会档案或借阅中的他会档案可读，返回 inline 资源 */
    DownloadFile openForView(Long attachId, long expires, String token);

    /**
     * 删除附件（逻辑删除）：仅商协会管理员可删本会草稿/审核退回档案的附件；
     * 尚未绑定档案的上传件仅上传者本人可删。
     */
    void delete(Long attachId);
}
