package com.wechatapply.associationwechatapply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.dto.ArchiveDetail;
import com.wechatapply.associationwechatapply.dto.ArchiveSubmitRequest;
import com.wechatapply.associationwechatapply.dto.ReviewRequest;
import com.wechatapply.associationwechatapply.entity.BizArchive;

/**
 * 档案条目服务（SRS FR-04/FR-06）。
 */
public interface ArchiveService {

    /** 分页查询：行级隔离（商协会仅本会、草稿仅本人）+ 元数据组合检索（SRS 4.2 / FR-07-01） */
    IPage<BizArchive> page(Page<BizArchive> page, Integer status, String majorCode, String archiveYear,
                           Integer securityLevel, String categoryCode, String assocCode, String keyword);

    /** 档案详情：元数据 + 附件清单 + 审核记录（查看/审核前核对） */
    ArchiveDetail detail(Long bizId);

    /** 填报：创建草稿或直接提交送审（UC-04） */
    BizArchive create(ArchiveSubmitRequest req);

    /** 修改草稿或退回件：可继续存草稿，也可直接提交送审（SRS FR-04-05） */
    BizArchive update(Long bizId, ArchiveSubmitRequest req);

    /** 补正后重新送审（退回件在原条目上修改，SRS FR-04-05） */
    BizArchive resubmit(Long bizId, ArchiveSubmitRequest req);

    /** 在线审核：通过并归档 / 退回修改（UC-06） */
    void review(ReviewRequest req);

    /** 删除已归档档案（仅主管部门操作员，逻辑删除并留审计日志，SRS FR-10-04） */
    void delete(Long bizId);
}
