package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.ArchiveDetail;
import com.wechatapply.associationwechatapply.dto.ArchiveSubmitRequest;
import com.wechatapply.associationwechatapply.dto.ImportResult;
import com.wechatapply.associationwechatapply.dto.ReviewRequest;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.service.ArchiveImportService;
import com.wechatapply.associationwechatapply.service.ArchiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 档案条目接口（SRS FR-04 / FR-06）。
 * 行级隔离在 ArchiveServiceImpl 内统一实施，控制器不重复判断。
 */
@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;
    private final ArchiveImportService archiveImportService;

    /** 档案列表：三类角色均可访问，数据范围由服务层过滤；支持元数据组合检索（SRS FR-07-01） */
    @GetMapping("/page")
    public R<IPage<BizArchive>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "20") long size,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) String majorCode,
                                     @RequestParam(required = false) String archiveYear,
                                     @RequestParam(required = false) Integer securityLevel,
                                     @RequestParam(required = false) String categoryCode,
                                     @RequestParam(required = false) String assocCode,
                                     @RequestParam(required = false) String keyword) {
        return R.ok(archiveService.page(Page.of(page, size), status, majorCode, archiveYear,
                securityLevel, categoryCode, assocCode, keyword));
    }

    /** 下载批量导入模板（SRS FR-04-04） */
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        archiveImportService.writeTemplate(out);
        String filename = URLEncoder.encode("档案导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(out.toByteArray());
    }

    /** Excel 批量导入档案：逐行校验并返回回执（仅商协会管理员） */
    @PostMapping("/import")
    @RequireRole(RoleCode.ASSOC)
    @AuditLog(action = "批量导入档案")
    public R<ImportResult> importExcel(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "false") boolean submit) {
        return R.ok(archiveImportService.importArchives(file, submit));
    }

    /** 档案详情：元数据、附件清单与审核记录，供“查看”与审核核对（三类角色，服务层按权限过滤） */
    @GetMapping("/{bizId}")
    public R<ArchiveDetail> detail(@PathVariable Long bizId) {
        return R.ok(archiveService.detail(bizId));
    }

    /** 填报：存草稿或提交送审（仅商协会管理员，SRS 表 4-1） */
    @PostMapping
    @RequireRole(RoleCode.ASSOC)
    @AuditLog(action = "档案填报")
    public R<BizArchive> create(@Valid @RequestBody ArchiveSubmitRequest req) {
        return R.ok(archiveService.create(req));
    }

    /** 修改草稿或审核退回件：可继续保存，也可修改后直接送审（仅商协会管理员） */
    @PutMapping("/{bizId}")
    @RequireRole(RoleCode.ASSOC)
    @AuditLog(action = "档案修改")
    public R<BizArchive> update(@PathVariable Long bizId, @Valid @RequestBody ArchiveSubmitRequest req) {
        return R.ok(archiveService.update(bizId, req));
    }

    /** 退回件补正后重新送审 */
    @PutMapping("/{bizId}/resubmit")
    @RequireRole(RoleCode.ASSOC)
    @AuditLog(action = "档案重新送审")
    public R<BizArchive> resubmit(@PathVariable Long bizId, @Valid @RequestBody ArchiveSubmitRequest req) {
        return R.ok(archiveService.resubmit(bizId, req));
    }

    /** 删除已归档档案：仅主管部门操作员，逻辑删除并记录审计日志（SRS FR-10-04） */
    @DeleteMapping("/{bizId}")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "删除已归档档案")
    public R<Void> delete(@PathVariable Long bizId) {
        archiveService.delete(bizId);
        return R.ok();
    }

    /** 在线审核：通过并归档 / 退回（仅主管部门操作员） */
    @PostMapping("/review")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "档案审核")
    public R<Void> review(@Valid @RequestBody ReviewRequest req) {
        archiveService.review(req);
        return R.ok();
    }
}
