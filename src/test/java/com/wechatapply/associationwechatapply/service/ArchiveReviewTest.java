package com.wechatapply.associationwechatapply.service;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.dto.ReviewRequest;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizAttachment;
import com.wechatapply.associationwechatapply.entity.BizReview;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.LoginUser;
import com.wechatapply.associationwechatapply.mapper.BizArchiveMapper;
import com.wechatapply.associationwechatapply.mapper.BizAssociationMapper;
import com.wechatapply.associationwechatapply.mapper.BizAttachmentMapper;
import com.wechatapply.associationwechatapply.mapper.BizCategoryMapper;
import com.wechatapply.associationwechatapply.mapper.BizBorrowMapper;
import com.wechatapply.associationwechatapply.mapper.BizReviewMapper;
import com.wechatapply.associationwechatapply.service.impl.ArchiveServiceImpl;
import com.wechatapply.associationwechatapply.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * “通过并归档”功能测试（SRS UC-06 / 5.3 / 5.4）。
 * 覆盖：档号生成（含已删除档案占用流水号的情形）、附件哈希校验、退回意见校验、
 * 仅待审核可审、以及撞号重试。
 */
@ExtendWith(MockitoExtension.class)
class ArchiveReviewTest {

    private static final String PREFIX = "ZSA-A001-A01-2026-";

    @Mock private BizArchiveMapper archiveMapper;
    @Mock private BizAttachmentMapper attachMapper;
    @Mock private BizCategoryMapper categoryMapper;
    @Mock private BizAssociationMapper assocMapper;
    @Mock private BizReviewMapper reviewMapper;
    @Mock private BizBorrowMapper borrowMapper;
    @Mock private FileStorageServiceImpl fileStorageService;

    private ArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArchiveServiceImpl(archiveMapper, attachMapper, categoryMapper, assocMapper,
                reviewMapper, borrowMapper, fileStorageService, new com.fasterxml.jackson.databind.ObjectMapper());
        // 模拟主管部门操作员登录
        LoginContextHolder.set(new LoginUser(1L, "admin", "ADMIN", null));
    }

    @AfterEach
    void tearDown() {
        LoginContextHolder.clear();
    }

    private BizArchive pendingArchive() {
        BizArchive a = new BizArchive();
        a.setBizId(6L);
        a.setAssocCode("A001");
        a.setCategoryCode("A01");
        a.setMajorCode("A");
        a.setArchiveYear("2026");
        a.setTitle("测试档案");
        a.setStatus(ArchiveServiceImpl.ST_PENDING);
        return a;
    }

    private ReviewRequest passRequest(BizArchive a) {
        ReviewRequest req = new ReviewRequest();
        req.setBizId(a.getBizId());
        req.setPass(true);
        req.setOpinion("同意归档");
        return req;
    }

    private void givenPendingWithNoAttachments(BizArchive a) {
        when(archiveMapper.selectOne(any())).thenReturn(a);
        when(attachMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("通过归档：档号取已用最大流水号+1，跳过被已删除档案占用的编号")
    void archiveUsesNextSeqIgnoringDeletedRecords() {
        BizArchive a = pendingArchive();
        givenPendingWithNoAttachments(a);
        // 库中已存在 000001（该行已逻辑删除，但仍占用唯一索引），新档号应为 000002
        when(archiveMapper.maxArchiveSeq(PREFIX)).thenReturn(1L);

        service.review(passRequest(a));

        assertEquals("ZSA-A001-A01-2026-000002", a.getArchiveId());
        assertEquals(ArchiveServiceImpl.ST_ARCHIVED, a.getStatus());
        assertEquals("1", a.getArchivedBy());
        assertTrue(a.getArchivedAt() != null);
        verify(reviewMapper).insert(any(BizReview.class));
        verify(archiveMapper).updateById(eq(a));
    }

    @Test
    @DisplayName("通过归档：首次档号撞唯一键时自动重新取号并成功")
    void archiveRetriesWhenCodeCollides() {
        BizArchive a = pendingArchive();
        givenPendingWithNoAttachments(a);
        when(archiveMapper.maxArchiveSeq(PREFIX)).thenReturn(1L, 2L);
        when(archiveMapper.updateById(any(BizArchive.class)))
                .thenThrow(new DuplicateKeyException("uk_archive_id"))
                .thenReturn(1);

        service.review(passRequest(a));

        assertEquals("ZSA-A001-A01-2026-000003", a.getArchiveId());
        verify(archiveMapper, times(2)).updateById(eq(a));
    }

    @Test
    @DisplayName("通过归档：附件哈希校验失败时中止，不更新档案")
    void archiveFailsWhenAttachmentHashMismatch() {
        BizArchive a = pendingArchive();
        when(archiveMapper.selectOne(any())).thenReturn(a);
        BizAttachment att = new BizAttachment();
        att.setAttachId(9L);
        att.setFileName("材料.pdf");
        when(attachMapper.selectList(any())).thenReturn(List.of(att));
        when(fileStorageService.verifyHash(att)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.review(passRequest(a)));
        assertTrue(ex.getMessage().contains("附件哈希校验失败"));
        verify(archiveMapper, never()).updateById(any(BizArchive.class));
    }

    @Test
    @DisplayName("退回：未填写审核意见时拒绝")
    void rejectRequiresOpinion() {
        BizArchive a = pendingArchive();
        when(archiveMapper.selectOne(any())).thenReturn(a);
        ReviewRequest req = new ReviewRequest();
        req.setBizId(a.getBizId());
        req.setPass(false);
        req.setOpinion("  ");

        assertThrows(BusinessException.class, () -> service.review(req));
        verify(archiveMapper, never()).updateById(any(BizArchive.class));
    }

    @Test
    @DisplayName("退回：填写意见后状态置为审核退回并记录原因")
    void rejectUpdatesStatusAndReason() {
        BizArchive a = pendingArchive();
        when(archiveMapper.selectOne(any())).thenReturn(a);
        ReviewRequest req = new ReviewRequest();
        req.setBizId(a.getBizId());
        req.setPass(false);
        req.setOpinion("缺少财务附件");

        service.review(req);

        assertEquals(ArchiveServiceImpl.ST_REJECTED, a.getStatus());
        assertEquals("缺少财务附件", a.getRejectReason());
        verify(archiveMapper).updateById(eq(a));
    }

    @Test
    @DisplayName("仅待审核档案可审核：已归档再审核被拒绝")
    void onlyPendingCanBeReviewed() {
        BizArchive a = pendingArchive();
        a.setStatus(ArchiveServiceImpl.ST_ARCHIVED);
        when(archiveMapper.selectOne(any())).thenReturn(a);

        assertThrows(BusinessException.class, () -> service.review(passRequest(a)));
        verify(archiveMapper, never()).updateById(any(BizArchive.class));
    }

    @Test
    @DisplayName("档号生成：同一协会、分类、年度下流水号递增且格式正确")
    void archiveCodeFormat() {
        BizArchive a = pendingArchive();
        givenPendingWithNoAttachments(a);
        when(archiveMapper.maxArchiveSeq(anyString())).thenReturn(136L);

        service.review(passRequest(a));

        assertEquals("ZSA-A001-A01-2026-000137", a.getArchiveId());
    }
}
