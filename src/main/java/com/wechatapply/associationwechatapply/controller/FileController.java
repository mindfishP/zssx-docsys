package com.wechatapply.associationwechatapply.controller;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.dto.DownloadFile;
import com.wechatapply.associationwechatapply.entity.BizAttachment;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 附件接口（SRS FR-04-03 / FR-10-03）。
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 附件上传：assoc 角色仅可上传本会目录，主管部门可代录 */
    @PostMapping("/upload")
    @RequireRole({RoleCode.ASSOC, RoleCode.ADMIN})
    @AuditLog(action = "附件上传")
    public R<BizAttachment> upload(@RequestParam(required = false) Long bizId,
                                   @RequestParam(required = false) String majorCode,
                                   @RequestParam(required = false) String assocCode,
                                   @RequestParam("file") MultipartFile file) {
        return R.ok(fileStorageService.upload(bizId, majorCode, assocCode, file));
    }

    /** 获取时效签名访问地址（默认 30 分钟有效，SRS FR-10-03） */
    @GetMapping("/{attachId}/signed-url")
    public R<String> signedUrl(@PathVariable Long attachId) {
        return R.ok(fileStorageService.signedUrl(attachId));
    }

    /** 借阅查阅签名地址：仅用于在线查看，不提供另存为 */
    @GetMapping("/{attachId}/signed-view-url")
    public R<String> signedViewUrl(@PathVariable Long attachId) {
        return R.ok(fileStorageService.signedViewUrl(attachId));
    }

    /**
     * 借阅查阅：本会档案或借阅中（未到期）的他会档案可读，
     * 统一 inline 返回并禁止内容嗅探，前端不提供下载入口。
     */
    @GetMapping("/view/{attachId}")
    public ResponseEntity<Resource> view(@PathVariable Long attachId,
                                         @RequestParam long expires,
                                         @RequestParam String token) {
        DownloadFile file = fileStorageService.openForView(attachId, expires, token);
        String encodedName = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    /** 删除附件：仅商协会管理员，限本会草稿/审核退回档案的附件（逻辑删除） */
    @DeleteMapping("/{attachId}")
    @RequireRole({RoleCode.ASSOC, RoleCode.ADMIN})
    @AuditLog(action = "删除附件")
    public R<Void> delete(@PathVariable Long attachId) {
        fileStorageService.delete(attachId);
        return R.ok();
    }

    /**
     * 附件打开/下载：校验签名有效期、签名与登录用户绑定、协会行级隔离后流式返回文件。
     * 采用 inline 方式返回，PDF/图片由浏览器直接预览，Office 文件由浏览器下载后打开。
     */
    @GetMapping("/download/{attachId}")
    public ResponseEntity<Resource> download(@PathVariable Long attachId,
                                             @RequestParam long expires,
                                             @RequestParam String token) {
        DownloadFile file = fileStorageService.openForDownload(attachId, expires, token);
        String encodedName = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.resource());
    }

    /**
     * 附件接口的异常按真实 HTTP 状态返回，响应体仍是统一的 {code,message,data}：
     * 前端以 blob 方式取文件时靠状态码判断成败，同时又能提示具体原因。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleFileError(BusinessException e) {
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null || status.is2xxSuccessful()) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(R.fail(e.getCode(), e.getMessage()));
    }
}
