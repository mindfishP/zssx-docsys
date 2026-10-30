package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.HashUtil;
import com.wechatapply.associationwechatapply.dto.DownloadFile;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizBorrow;
import com.wechatapply.associationwechatapply.entity.BizAttachment;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.mapper.BizArchiveMapper;
import com.wechatapply.associationwechatapply.mapper.BizBorrowMapper;
import com.wechatapply.associationwechatapply.mapper.BizAttachmentMapper;
import com.wechatapply.associationwechatapply.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * 附件存储：内容直接保存在数据库（biz_attachment.file_content），
 * 数据库迁移或换机后附件仍可打开，不再依赖服务器本地目录；
 * 历史数据仍保留 store_path，读取时回退到磁盘文件。
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final BizAttachmentMapper attachMapper;
    private final BizArchiveMapper archiveMapper;
    private final BizBorrowMapper borrowMapper;

    /** 历史数据磁盘根目录（仅兼容旧附件） */
    @Value("${zssx.storage.root}")
    private String storageRoot;
    @Value("${zssx.storage.url-expire-minutes:30}")
    private long urlExpireMinutes;

    /** 允许的附件格式（SRS FR-04-03） */
    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "zip", "rar");
    /**
     * 入库单文件上限：受 MySQL max_allowed_packet（默认 64MB）限制，
     * 取 60MB 留出余量；超过时提示改用更小文件。
     */
    private static final long MAX_DB_SIZE = 60L * 1024 * 1024;

    @Override
    public BizAttachment upload(Long bizId, String majorCode, String assocCode, MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = extOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ResultCode.ATTACHMENT_TYPE_INVALID, "不支持的文件格式: " + ext);
        }
        if (file.getSize() > MAX_DB_SIZE) {
            throw new BusinessException(ResultCode.ATTACHMENT_TOO_LARGE,
                    "单个附件不能超过 60MB（附件内容随数据库保存）");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ATTACHMENT_UPLOAD_FAILED, "读取上传文件失败");
        }
        BizAttachment att = new BizAttachment();
        att.setBizId(bizId);
        att.setFileName(original);
        att.setStorePath(null);                 // 新附件不再落磁盘
        att.setFileContent(content);            // 内容入库，随数据库迁移
        att.setFileType(ext);
        att.setFileSize((long) content.length);
        att.setUploadBy(String.valueOf(LoginContextHolder.getUserId()));
        att.setUploadAt(java.time.LocalDateTime.now());
        att.setFileHash(HashUtil.sha256Hex(content));
        attachMapper.insert(att);
        return att;
    }

    @Override
    public boolean verifyHash(BizAttachment att) {
        if (att.getFileHash() == null) {
            return false;
        }
        // 新数据：直接校验库中的二进制内容
        if (att.getFileContent() != null) {
            return att.getFileHash().equals(HashUtil.sha256Hex(att.getFileContent()));
        }
        // 历史数据：回退到磁盘文件校验
        Path p = legacyPath(att.getStorePath());
        if (p == null || !Files.isRegularFile(p)) {
            return false;
        }
        return att.getFileHash().equals(hashOf(p));
    }

    /**
     * 时效访问签名（SRS FR-10-03）：
     * 返回 /api/file/download/{id}?expires=…&token=… 形式地址；
     * token 与登录用户绑定，即使链接被转发也不能被他人打开。
     */
    @Override
    public String signedUrl(Long attachId) {
        BizAttachment att = attachMapper.selectById(attachId);
        if (att == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "附件不存在");
        }
        assertOwnerAccess(att);
        long expires = System.currentTimeMillis() + urlExpireMinutes * 60_000L;
        String token = HashUtil.sha256Hex((attachId + ":" + expires + ":" + LoginContextHolder.getUserId())
                .getBytes(StandardCharsets.UTF_8)).substring(0, 32);
        return "/api/file/download/" + attachId + "?expires=" + expires + "&token=" + token;
    }

    /**
     * 借阅查阅签名：token 加入 view 盐，与下载签名不可互换。
     */
    @Override
    public String signedViewUrl(Long attachId) {
        BizAttachment att = attachMapper.selectById(attachId);
        if (att == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "附件不存在");
        }
        assertViewAccess(att);
        long expires = System.currentTimeMillis() + urlExpireMinutes * 60_000L;
        String token = HashUtil.sha256Hex((attachId + ":" + expires + ":" + LoginContextHolder.getUserId() + ":view")
                .getBytes(StandardCharsets.UTF_8)).substring(0, 32);
        return "/api/file/view/" + attachId + "?expires=" + expires + "&token=" + token;
    }

    /**
     * 借阅查阅：本会档案、主管部门/领导、或借阅中（未到期）的他会档案可读；
     * 一律以 inline 返回，前端不提供下载入口。
     */
    @Override
    public DownloadFile openForView(Long attachId, long expires, String token) {
        if (expires < System.currentTimeMillis()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "访问链接已过期，请重新打开");
        }
        String expect = HashUtil.sha256Hex((attachId + ":" + expires + ":" + LoginContextHolder.getUserId() + ":view")
                .getBytes(StandardCharsets.UTF_8)).substring(0, 32);
        if (!expect.equals(token)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "访问签名无效");
        }
        BizAttachment att = attachMapper.selectById(attachId);
        if (att == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "附件不存在");
        }
        assertViewAccess(att);
        Resource resource;
        if (att.getFileContent() != null && att.getFileContent().length > 0) {
            resource = new ByteArrayResource(att.getFileContent());
        } else {
            Path file = legacyPath(att.getStorePath());
            if (file == null || !Files.isRegularFile(file)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "附件文件不存在或已被移动");
            }
            resource = new FileSystemResource(file);
        }
        String type = CONTENT_TYPES.getOrDefault(
                att.getFileType() == null ? "" : att.getFileType().toLowerCase(),
                "application/octet-stream");
        return new DownloadFile(resource, att.getFileName(), type, true);
    }

    /** 常见附件格式的 MIME 映射，保证 PDF/图片在浏览器内直接打开（SRS FR-04-03） */
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/vnd.rar"));

    /** 下载权限：主管部门/领导不限，协会仅限本会档案附件 */
    private void assertOwnerAccess(BizAttachment att) {
        String own = LoginContextHolder.getAssocCode();
        if (own == null) {
            return;
        }
        BizArchive archive = att.getBizId() == null ? null : archiveMapper.selectById(att.getBizId());
        if (archive == null || !own.equals(archive.getAssocCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该附件");
        }
    }

    /** 查阅权限：本会档案、或借阅中（未到期）的他会档案 */
    private void assertViewAccess(BizAttachment att) {
        String own = LoginContextHolder.getAssocCode();
        if (own == null) {
            return;
        }
        BizArchive archive = att.getBizId() == null ? null : archiveMapper.selectById(att.getBizId());
        if (archive != null && own.equals(archive.getAssocCode())) {
            return;
        }
        Long active = archive == null ? 0L : borrowMapper.selectCount(Wrappers.<BizBorrow>lambdaQuery()
                .eq(BizBorrow::getBizId, archive.getBizId())
                .eq(BizBorrow::getApplicantAssoc, own)
                .eq(BizBorrow::getBorrowStatus, 1)
                .gt(BizBorrow::getDueAt, LocalDateTime.now()));
        if (active == null || active == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该档案未借阅或借阅已到期，无法查看附件");
        }
    }

    @Override
    public DownloadFile openForDownload(Long attachId, long expires, String token) {
        if (expires < System.currentTimeMillis()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "访问链接已过期，请重新打开");
        }
        // 签名与当前登录用户绑定，链接被转发给他人也无法使用
        String expect = HashUtil.sha256Hex((attachId + ":" + expires + ":" + LoginContextHolder.getUserId())
                .getBytes(StandardCharsets.UTF_8)).substring(0, 32);
        if (!expect.equals(token)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "访问签名无效");
        }
        BizAttachment att = attachMapper.selectById(attachId);
        if (att == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "附件不存在");
        }
        // 行级隔离：商协会只能访问本会档案的附件
        String own = LoginContextHolder.getAssocCode();
        if (own != null) {
            BizArchive archive = att.getBizId() == null ? null : archiveMapper.selectById(att.getBizId());
            if (archive == null || !own.equals(archive.getAssocCode())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该附件");
            }
        }
        Resource resource;
        if (att.getFileContent() != null && att.getFileContent().length > 0) {
            resource = new ByteArrayResource(att.getFileContent());
        } else {
            // 历史数据兼容：内容仍在磁盘时按相对路径读取
            Path file = legacyPath(att.getStorePath());
            if (file == null || !Files.isRegularFile(file)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "附件文件不存在或已被移动");
            }
            resource = new FileSystemResource(file);
        }
        String type = CONTENT_TYPES.getOrDefault(
                att.getFileType() == null ? "" : att.getFileType().toLowerCase(),
                "application/octet-stream");
        return new DownloadFile(resource, att.getFileName(), type);
    }

    @Override
    public void delete(Long attachId) {
        BizAttachment att = attachMapper.selectById(attachId);
        if (att == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "附件不存在");
        }
        String me = String.valueOf(LoginContextHolder.getUserId());
        if (att.getBizId() == null) {
            // 尚未绑定档案的上传件：只允许上传者本人删除
            if (!me.equals(att.getUploadBy())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能删除本人上传的附件");
            }
        } else {
            BizArchive archive = archiveMapper.selectById(att.getBizId());
            if (archive == null) {
                throw new BusinessException(ResultCode.ARCHIVE_NOT_FOUND);
            }
            String own = LoginContextHolder.getAssocCode();
            if (own != null && !own.equals(archive.getAssocCode())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "只能删除本会档案的附件");
            }
            // 未归档前（草稿/待审核/审核退回）可调整附件；已归档档案作为正式档案不可再改
            Integer status = archive.getStatus();
            if (status == null || status == ArchiveServiceImpl.ST_ARCHIVED) {
                throw new BusinessException(ResultCode.ARCHIVE_STATUS_INVALID,
                        "已归档的档案不能删除附件");
            }
        }
        attachMapper.deleteById(attachId);   // 逻辑删除，保留审计痕迹
    }

    /** 历史附件的磁盘绝对路径；越出存储根目录或未配置时返回 null */
    private Path legacyPath(String storePath) {        if (storePath == null || storePath.isBlank() || storageRoot == null || storageRoot.isBlank()) {
            return null;
        }
        Path base = Paths.get(storageRoot).toAbsolutePath().normalize();
        Path file = base.resolve(storePath.replaceFirst("^/+", "")).normalize();
        return file.startsWith(base) ? file : null;
    }

    private String hashOf(Path p) {
        try (InputStream in = Files.newInputStream(p)) {
            return HashUtil.sha256Hex(in);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.ATTACHMENT_UPLOAD_FAILED, "读取文件计算哈希失败");
        }
    }

    private String extOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
