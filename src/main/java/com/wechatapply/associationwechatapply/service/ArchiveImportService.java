package com.wechatapply.associationwechatapply.service;

import com.wechatapply.associationwechatapply.dto.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 档案批量导入（SRS FR-04-04）：模板下载 + Excel 导入与校验回执。
 */
public interface ArchiveImportService {

    /** 生成导入模板（xlsx）到输出流 */
    void writeTemplate(OutputStream out) throws IOException;

    /** 解析并导入；submit=true 表示逐行提交送审，否则存草稿 */
    ImportResult importArchives(MultipartFile file, boolean submit);
}
