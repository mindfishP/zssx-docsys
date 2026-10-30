package com.wechatapply.associationwechatapply.service.impl;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.SimpleXlsx;
import com.wechatapply.associationwechatapply.dto.ArchiveSubmitRequest;
import com.wechatapply.associationwechatapply.dto.ImportResult;
import com.wechatapply.associationwechatapply.service.ArchiveImportService;
import com.wechatapply.associationwechatapply.service.ArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 档案批量导入实现：
 * 模板列固定为「分类编码 / 题名 / 年度 / 责任者 / 密级 / 保管期限」，
 * 逐行调用 {@link ArchiveService#create} 复用既有的分类校验、元数据必填校验与行级隔离。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveImportServiceImpl implements ArchiveImportService {

    /** 模板表头（顺序即列顺序） */
    public static final List<String> HEADERS =
            List.of("分类编码", "题名", "年度", "责任者", "密级", "保管期限");

    /** 单次导入行数上限 */
    public static final int MAX_ROWS = 500;

    private static final Set<String> RETENTIONS = Set.of("永久", "定期30年", "定期10年");

    private final ArchiveService archiveService;

    @Override
    public void writeTemplate(OutputStream out) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        rows.add(List.of("C01", "2026 年度会员名册", String.valueOf(LocalDate.now().getYear()),
                "张三", "公开", "定期30年"));
        rows.add(List.of("D01", "2026 年第一季度会员活动材料", String.valueOf(LocalDate.now().getYear()),
                "李四", "内部", "定期10年"));
        SimpleXlsx.write(out, "档案导入模板", rows);
    }

    @Override
    public ImportResult importArchives(MultipartFile file, boolean submit) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请上传 Excel 文件");
        }
        List<List<String>> rows;
        try (InputStream in = file.getInputStream()) {
            rows = SimpleXlsx.read(in);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无法读取 Excel 文件：" + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件内容为空");
        }
        checkHeader(rows.get(0));
        if (rows.size() - 1 > MAX_ROWS) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单次最多导入 " + MAX_ROWS + " 行");
        }

        ImportResult result = new ImportResult();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            result.setTotal(result.getTotal() + 1);
            int rowNo = i + 1;   // 与 Excel 行号一致
            try {
                archiveService.create(toRequest(row, submit));
                result.setSuccess(result.getSuccess() + 1);
            } catch (BusinessException e) {
                result.addError(rowNo, e.getMessage());
            } catch (Exception e) {
                log.warn("第 {} 行导入失败", rowNo, e);
                result.addError(rowNo, "导入失败：" + e.getMessage());
            }
        }
        return result;
    }

    private void checkHeader(List<String> header) {
        for (int i = 0; i < HEADERS.size(); i++) {
            String actual = i < header.size() ? header.get(i).trim() : "";
            if (!HEADERS.get(i).equals(actual)) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "表头不符，请使用下载的模板（第 " + (i + 1) + " 列应为「" + HEADERS.get(i) + "」）");
            }
        }
    }

    private ArchiveSubmitRequest toRequest(List<String> row, boolean submit) {
        String category = cell(row, 0);
        String title = cell(row, 1);
        String year = cell(row, 2);
        String responsibility = cell(row, 3);
        String level = cell(row, 4);
        String retention = cell(row, 5);

        if (category.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类编码不能为空");
        }
        if (title.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "题名不能为空");
        }
        if (!year.isEmpty() && !year.matches("\\d{4}")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "年度应为 4 位年份，如 2026");
        }
        ArchiveSubmitRequest req = new ArchiveSubmitRequest();
        req.setCategoryCode(category);
        req.setTitle(title);
        req.setArchiveYear(year.isEmpty() ? String.valueOf(LocalDate.now().getYear()) : year);
        req.setResponsibility(responsibility.isEmpty() ? null : responsibility);
        req.setSecurityLevel(parseLevel(level));
        if (!retention.isEmpty()) {
            if (!RETENTIONS.contains(retention)) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "保管期限只能填 永久 / 定期30年 / 定期10年");
            }
            req.setRetention(retention);
        }
        req.setSubmit(submit);
        return req;
    }

    private Integer parseLevel(String level) {
        if (level == null || level.isEmpty()) {
            return null;   // 取分类默认密级
        }
        return switch (level) {
            case "公开", "1" -> 1;
            case "内部", "2" -> 2;
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "密级只能填 公开 / 内部");
        };
    }

    private String cell(List<String> row, int index) {
        return index < row.size() && row.get(index) != null ? row.get(index).trim() : "";
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(v -> v == null || v.isBlank());
    }
}
