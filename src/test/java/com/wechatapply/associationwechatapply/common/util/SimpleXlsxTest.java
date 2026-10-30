package com.wechatapply.associationwechatapply.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 轻量 XLSX 读写：模板生成与解析回读一致性（SRS FR-04-04）。
 */
class SimpleXlsxTest {

    @Test
    @DisplayName("写入后可原样读回：中文、空单元格、含特殊字符的文本")
    void writeThenRead() throws Exception {
        List<List<String>> rows = List.of(
                List.of("分类编码", "题名", "年度", "责任者"),
                List.of("C01", "2026 年度会员名册", "2026", "张三"),
                List.of("D01", "A&B <测试> 活动", "2026", ""),
                List.of("", "", "", "")
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SimpleXlsx.write(out, "档案导入模板", rows);

        List<List<String>> read = SimpleXlsx.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(4, read.size());
        assertEquals(List.of("分类编码", "题名", "年度", "责任者"), read.get(0));
        assertEquals("2026 年度会员名册", read.get(1).get(1));
        assertEquals("A&B <测试> 活动", read.get(2).get(1));
        // 末尾空单元格不写出，读取时行长度会少于表头列数；导入侧按列容错读取
        assertEquals(3, read.get(2).size());
    }

    @Test
    @DisplayName("生成的是合法 xlsx 包（含工作簿与工作表部件）")
    void producesValidPackage() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SimpleXlsx.write(out, "s", List.of(List.of("a")));
        byte[] bytes = out.toByteArray();
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
        java.util.Set<String> entries = new java.util.HashSet<>();
        try (java.util.zip.ZipInputStream zip =
                     new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                entries.add(e.getName());
            }
        }
        assertTrue(entries.contains("[Content_Types].xml"));
        assertTrue(entries.contains("xl/workbook.xml"));
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"));
    }

    @Test
    @DisplayName("缺少工作表时给出明确错误")
    void rejectsNonXlsx() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("readme.txt"));
            zip.write("not excel".getBytes());
            zip.closeEntry();
        } catch (Exception ignored) {
            // ignore
        }
        Exception ex = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> SimpleXlsx.read(new ByteArrayInputStream(out.toByteArray())));
        assertTrue(ex.getMessage().contains("xlsx"));
    }
}
