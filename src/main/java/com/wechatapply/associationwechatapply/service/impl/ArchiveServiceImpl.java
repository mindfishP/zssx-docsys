package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.ArchiveCodeUtil;
import com.wechatapply.associationwechatapply.dto.ArchiveDetail;
import com.wechatapply.associationwechatapply.dto.ArchiveSubmitRequest;
import com.wechatapply.associationwechatapply.dto.ReviewRequest;
import com.wechatapply.associationwechatapply.entity.*;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.mapper.*;
import com.wechatapply.associationwechatapply.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    /** 条目状态常量（SRS 2.4 状态机） */
    public static final int ST_DRAFT = 0;
    public static final int ST_PENDING = 1;
    public static final int ST_REJECTED = 2;
    public static final int ST_ARCHIVED = 3;

    private final BizArchiveMapper archiveMapper;
    private final BizAttachmentMapper attachMapper;
    private final BizCategoryMapper categoryMapper;
    private final BizAssociationMapper assocMapper;
    private final BizReviewMapper reviewMapper;
    private final BizBorrowMapper borrowMapper;
    private final FileStorageServiceImpl fileStorageService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<BizArchive> page(Page<BizArchive> page, Integer status, String majorCode,
                                  String archiveYear, Integer securityLevel, String categoryCode,
                                  String assocCode, String keyword) {
        // 行级隔离：商协会管理员只能看本会数据（SRS 4.2 规则 1）；
        // 草稿仅创建人本人可见（SRS FR-04-05），其余状态本会可见。
        String own = LoginContextHolder.getAssocCode();
        String me = String.valueOf(LoginContextHolder.getUserId());
        return archiveMapper.selectPage(page, Wrappers.<BizArchive>lambdaQuery()
                .eq(status != null, BizArchive::getStatus, status)
                .eq(majorCode != null && !majorCode.isBlank(), BizArchive::getMajorCode, majorCode)
                .eq(archiveYear != null && !archiveYear.isBlank(), BizArchive::getArchiveYear, archiveYear)
                .eq(securityLevel != null, BizArchive::getSecurityLevel, securityLevel)
                .eq(categoryCode != null && !categoryCode.isBlank(), BizArchive::getCategoryCode, categoryCode)
                .eq(own != null, BizArchive::getAssocCode, own)
                .eq(own == null && StringUtils.isNotBlank(assocCode), BizArchive::getAssocCode, assocCode)
                .and(w -> w.ne(BizArchive::getStatus, ST_DRAFT)
                        .or().eq(BizArchive::getCreateBy, me))
                // 关键词同时匹配题名、责任者与附件文件名（SRS FR-07-01 / FR-07-02）
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(BizArchive::getTitle, keyword)
                        .or().like(BizArchive::getResponsibility, keyword)
                        .or().apply("exists (select 1 from biz_attachment a "
                                + "where a.biz_id = biz_archive.biz_id and a.deleted = 0 "
                                + "and a.file_name like concat('%', {0}, '%'))", keyword))
                .orderByDesc(BizArchive::getBizId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizArchive create(ArchiveSubmitRequest req) {
        String assocCode = LoginContextHolder.getAssocCode();
        if (assocCode == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅商协会管理员可填报档案");
        }
        BizCategory cat = categoryMapper.selectById(req.getCategoryCode());
        if (cat == null || cat.getStatus() == null || cat.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "档案分类不存在或已停用");
        }
        if (req.isSubmit()) {
            validateRequiredMetadata(cat, req);   // 送审前按元数据字典校验必填项
        }
        BizArchive a = new BizArchive();
        a.setAssocCode(assocCode);
        a.setCategoryCode(cat.getCategoryCode());
        a.setMajorCode(cat.getMajorCode());
        a.setTitle(req.getTitle());
        a.setArchiveYear(req.getArchiveYear());
        a.setResponsibility(req.getResponsibility());
        a.setCreateBy(String.valueOf(LoginContextHolder.getUserId()));   // 草稿仅本人可见
        a.setSecurityLevel(req.getSecurityLevel() != null ? req.getSecurityLevel() : cat.getDefaultSecurityLevel());
        a.setRetention(req.getRetention() != null ? req.getRetention() : cat.getDefaultRetention());
        a.setStatus(req.isSubmit() ? ST_PENDING : ST_DRAFT);
        if (req.isSubmit()) {
            a.setSubmitBy(String.valueOf(LoginContextHolder.getUserId()));
            a.setSubmitAt(LocalDateTime.now());
        }
        archiveMapper.insert(a);

        // 回填附件归属并校验哈希已登记
        bindAttachments(a.getBizId(), req.getAttachIds());
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizArchive update(Long bizId, ArchiveSubmitRequest req) {
        BizArchive a = mustGetOwned(bizId);
        // 仅草稿与审核退回件可修改，待审核与已归档条目由审核/退回流程驱动
        if (a.getStatus() != ST_DRAFT && a.getStatus() != ST_REJECTED) {
            throw new BusinessException(ResultCode.ARCHIVE_STATUS_INVALID, "仅草稿或审核退回的档案可修改");
        }
        // 草稿仅创建人本人可见，也只能由本人修改
        if (a.getStatus() == ST_DRAFT && !isCreator(a)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "草稿仅创建人本人可修改");
        }
        BizCategory cat = categoryMapper.selectById(req.getCategoryCode());
        if (cat == null || cat.getStatus() == null || cat.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "档案分类不存在或已停用");
        }
        if (req.isSubmit()) {
            validateRequiredMetadata(cat, req);   // 送审前按元数据字典校验必填项
        }
        a.setCategoryCode(cat.getCategoryCode());
        a.setMajorCode(cat.getMajorCode());
        a.setTitle(req.getTitle());
        a.setArchiveYear(req.getArchiveYear());
        a.setResponsibility(req.getResponsibility());
        a.setSecurityLevel(req.getSecurityLevel() != null ? req.getSecurityLevel() : a.getSecurityLevel());
        a.setRetention(req.getRetention() != null ? req.getRetention() : a.getRetention());
        if (req.isSubmit()) {          // 修改后直接送审
            a.setStatus(ST_PENDING);
            a.setRejectReason(null);
            a.setSubmitBy(String.valueOf(LoginContextHolder.getUserId()));
            a.setSubmitAt(LocalDateTime.now());
        }
        archiveMapper.updateById(a);

        // 本次新上传的附件追加到该条目，原有附件保持不变
        bindAttachments(a.getBizId(), req.getAttachIds());
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizArchive resubmit(Long bizId, ArchiveSubmitRequest req) {
        BizArchive a = mustGetOwned(bizId);
        if (a.getStatus() != ST_REJECTED && a.getStatus() != ST_DRAFT) {
            throw new BusinessException(ResultCode.ARCHIVE_STATUS_INVALID);
        }
        if (a.getStatus() == ST_DRAFT && !isCreator(a)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "草稿仅创建人本人可送审");
        }
        a.setTitle(req.getTitle());
        a.setArchiveYear(req.getArchiveYear());
        a.setResponsibility(req.getResponsibility());
        a.setSecurityLevel(req.getSecurityLevel() != null ? req.getSecurityLevel() : a.getSecurityLevel());
        a.setRetention(req.getRetention() != null ? req.getRetention() : a.getRetention());
        a.setStatus(ST_PENDING);
        a.setRejectReason(null);
        a.setSubmitBy(String.valueOf(LoginContextHolder.getUserId()));
        a.setSubmitAt(LocalDateTime.now());
        archiveMapper.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(ReviewRequest req) {
        BizArchive a = archiveMapper.selectOne(Wrappers.<BizArchive>lambdaQuery()
                .eq(BizArchive::getBizId, req.getBizId()));
        if (a == null) {
            throw new BusinessException(ResultCode.ARCHIVE_NOT_FOUND);
        }
        if (a.getStatus() != ST_PENDING) {
            throw new BusinessException(ResultCode.ARCHIVE_STATUS_INVALID);
        }
        BizReview r = new BizReview();
        r.setBizId(a.getBizId());
        r.setArchiveId(a.getArchiveId());
        r.setNodeLevel(1);
        r.setReviewerId(LoginContextHolder.getUserId());
        r.setReviewerName(String.valueOf(LoginContextHolder.getUserId()));

        if (req.isPass()) {
            // 归档：生成唯一标识 + 复核附件哈希（SRS UC-06 / 5.3 / 5.4）
            r.setReviewResult(1);
            reviewMapper.insert(r);

            List<BizAttachment> atts = attachMapper.selectList(Wrappers.<BizAttachment>lambdaQuery()
                    .eq(BizAttachment::getBizId, a.getBizId()));
            for (BizAttachment att : atts) {
                if (!fileStorageService.verifyHash(att)) {
                    throw new BusinessException(ResultCode.ATTACHMENT_UPLOAD_FAILED,
                            "附件哈希校验失败: " + att.getFileName());
                }
            }
            a.setStatus(ST_ARCHIVED);
            a.setArchivedBy(String.valueOf(LoginContextHolder.getUserId()));
            a.setArchivedAt(LocalDateTime.now());
            archiveWithCode(a);
            return;
        } else {
            if (req.getOpinion() == null || req.getOpinion().isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "退回必须填写审核意见");
            }
            r.setReviewResult(2);
            r.setReviewOpinion(req.getOpinion());
            reviewMapper.insert(r);
            a.setStatus(ST_REJECTED);
            a.setRejectReason(req.getOpinion());
        }
        archiveMapper.updateById(a);
    }

    /**
     * 生成档号并归档落库。
     * 流水号取“已用最大号 + 1”，且统计范围包含逻辑删除的记录——已删除档案的档号仍占唯一索引，
     * 若只统计未删除记录会生成重复档号（表现为“通过并归档”时报系统错误）。
     * 并发下若仍撞号，重新取号重试最多 3 次。
     */
    private void archiveWithCode(BizArchive a) {
        String subCode = a.getCategoryCode().substring(1);
        String year = a.getArchiveYear() != null && !a.getArchiveYear().isBlank()
                ? a.getArchiveYear() : String.valueOf(LocalDate.now().getYear());
        String prefix = ArchiveCodeUtil.PREFIX + "-" + a.getAssocCode() + "-"
                + a.getMajorCode() + subCode + "-" + year + "-";
        int attempt = 0;
        while (true) {
            long seq = archiveMapper.maxArchiveSeq(prefix) + 1;
            a.setArchiveId(ArchiveCodeUtil.nextCode(
                    a.getAssocCode(), a.getMajorCode(), subCode, year, seq));
            try {
                archiveMapper.updateById(a);
                return;
            } catch (DuplicateKeyException e) {
                if (++attempt >= 3) {
                    throw new BusinessException(ResultCode.ARCHIVE_STATUS_INVALID,
                            "档案编号生成冲突，请稍后重试");
                }
            }
        }
    }

    @Override
    public ArchiveDetail detail(Long bizId) {
        BizArchive a = archiveMapper.selectById(bizId);
        if (a == null) {
            throw new BusinessException(ResultCode.ARCHIVE_NOT_FOUND);
        }
        String own = LoginContextHolder.getAssocCode();
        boolean owner = own == null || own.equals(a.getAssocCode());
        // 他会档案：仅借阅中且未到期时可查阅（SRS FR-07）
        if (!owner && !borrowedActive(bizId, own)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        // 草稿仅创建人本人可见（SRS FR-04-05）
        if (a.getStatus() != null && a.getStatus() == ST_DRAFT && !isCreator(a)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "草稿仅创建人本人可查看");
        }
        // 只取附件元数据，不把二进制内容查出来（内容在下载接口按需读取）
        List<BizAttachment> attachments = attachMapper.selectList(Wrappers.<BizAttachment>lambdaQuery()
                .select(BizAttachment::getAttachId, BizAttachment::getBizId, BizAttachment::getFileName,
                        BizAttachment::getFileType, BizAttachment::getFileSize, BizAttachment::getFileHash,
                        BizAttachment::getUploadBy, BizAttachment::getUploadAt)
                .eq(BizAttachment::getBizId, bizId)
                .orderByAsc(BizAttachment::getAttachId));
        List<BizReview> reviews = reviewMapper.selectList(Wrappers.<BizReview>lambdaQuery()
                .eq(BizReview::getBizId, bizId)
                .orderByAsc(BizReview::getReviewId));
        boolean borrowed = !owner && own != null;
        return new ArchiveDetail(a, attachments, reviews,
                new ArchiveDetail.Access(owner, borrowed, owner));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long bizId) {
        BizArchive a = archiveMapper.selectById(bizId);
        if (a == null) {
            throw new BusinessException(ResultCode.ARCHIVE_NOT_FOUND);
        }
        // 仅允许删除已归档档案；在办条目走审核/退回流程，避免误删
        if (a.getStatus() == null || a.getStatus() != ST_ARCHIVED) {
            throw new BusinessException(ResultCode.ARCHIVE_STATUS_INVALID, "仅已归档的档案可删除");
        }
        // 逻辑删除：记录保留在库中可追溯，操作由 @AuditLog 记入审计日志
        archiveMapper.deleteById(bizId);
        attachMapper.delete(Wrappers.<BizAttachment>lambdaQuery()
                .eq(BizAttachment::getBizId, bizId));
    }

    /** 是否存在本会针对该档案的借阅中记录（未到期） */
    private boolean borrowedActive(Long bizId, String assocCode) {
        if (assocCode == null) {
            return false;
        }
        Long count = borrowMapper.selectCount(Wrappers.<BizBorrow>lambdaQuery()
                .eq(BizBorrow::getBizId, bizId)
                .eq(BizBorrow::getApplicantAssoc, assocCode)
                .eq(BizBorrow::getBorrowStatus, 1)
                .gt(BizBorrow::getDueAt, LocalDateTime.now()));
        return count != null && count > 0;
    }

    /** 按分类的元数据字典校验必填项（SRS FR-03-02 / FR-04-01） */
    private void validateRequiredMetadata(BizCategory cat, ArchiveSubmitRequest req) {
        String json = cat.getRequiredMetaKeys();
        if (json == null || json.isBlank()) {
            return;
        }
        List<String> keys;
        try {
            keys = objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return;   // 字典配置异常时不阻断业务
        }
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            switch (key) {
                case "title" -> {
                    if (isBlank(req.getTitle())) {
                        missing.add("题名");
                    }
                }
                case "archiveYear" -> {
                    if (isBlank(req.getArchiveYear())) {
                        missing.add("年度");
                    }
                }
                case "responsibility" -> {
                    if (isBlank(req.getResponsibility())) {
                        missing.add("责任者");
                    }
                }
                default -> {
                    // 其他元数据项暂未建模，忽略
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ResultCode.ARCHIVE_METADATA_INCOMPLETE,
                    "缺少必填元数据：" + String.join("、", missing));
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 当前登录用户是否为该条目的创建人（草稿仅本人可见/可改） */
    private boolean isCreator(BizArchive a) {        return a.getCreateBy() != null
                && a.getCreateBy().equals(String.valueOf(LoginContextHolder.getUserId()));
    }

    /** 把本次上传的附件绑定到档案条目（附件在归档时才复核哈希，SRS 5.4） */
    private void bindAttachments(Long bizId, List<Long> attachIds) {        if (attachIds == null || attachIds.isEmpty()) {
            return;
        }
        for (Long attachId : attachIds) {
            BizAttachment att = attachMapper.selectById(attachId);
            if (att != null) {
                att.setBizId(bizId);
                attachMapper.updateById(att);
            }
        }
    }

    private BizArchive mustGetOwned(Long bizId) {
        BizArchive a = archiveMapper.selectOne(Wrappers.<BizArchive>lambdaQuery()
                .eq(BizArchive::getBizId, bizId));
        if (a == null) {
            throw new BusinessException(ResultCode.ARCHIVE_NOT_FOUND);
        }
        String own = LoginContextHolder.getAssocCode();
        if (own != null && !own.equals(a.getAssocCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return a;
    }
}
