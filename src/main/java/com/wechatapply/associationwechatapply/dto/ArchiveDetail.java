package com.wechatapply.associationwechatapply.dto;

import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizAttachment;
import com.wechatapply.associationwechatapply.entity.BizReview;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 档案详情视图（SRS FR-06 / FR-07）：档案元数据 + 附件清单 + 审核记录。
 * access 描述当前登录人对该档案的访问方式：本会可下载，借阅查阅仅可在线查看。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveDetail {

    private BizArchive archive;
    private List<BizAttachment> attachments;
    private List<BizReview> reviews;
    private Access access;

    /** owner=本会档案（可下载附件）；borrowed=借阅中（仅在线查阅）；canDownload 供前端控制按钮 */
    public record Access(boolean owner, boolean borrowed, boolean canDownload) {
    }
}