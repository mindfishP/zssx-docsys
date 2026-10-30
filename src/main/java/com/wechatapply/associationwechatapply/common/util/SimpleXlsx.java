package com.wechatapply.associationwechatapply.common.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 轻量 XLSX 读写（仅用 JDK，不依赖 POI）：
 * 提供导入模板生成与首个工作表内容读取，满足 SRS FR-04-04 的批量导入与模板下载。
 *
 * <p>写入使用 inlineStr；读取同时兼容 inlineStr、共享字符串（Excel 保存后文本会进 sharedStrings）
 * 与公式结果（str），足以覆盖用户手工填写后的模板文件。</p>
 */
public final class SimpleXlsx {

    private static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

    private SimpleXlsx() {
    }

    /**
     * 生成单表 XLSX。
     *
     * @param sheetName 工作表名
     * @param rows      行数据，第一行通常为表头
     */
    public static void write(OutputStream out, String sheetName, List<List<String>> rows) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook(sheetName));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/styles.xml", styles());
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(rows));
        }
    }

    /**
     * 读取首个工作表内容，返回“行 × 列”的纯文本矩阵；空单元格返回空串。
     */
    public static List<List<String>> read(InputStream in) throws IOException {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                parts.put(entry.getName(), zip.readAllBytes());
            }
        }
        byte[] sheet = parts.get("xl/worksheets/sheet1.xml");
        if (sheet == null) {
            throw new IOException("不是有效的 xlsx 文件：缺少工作表");
        }
        List<String> shared = parseSharedStrings(parts.get("xl/sharedStrings.xml"));
        Document doc = parse(sheet);
        NodeList rowNodes = doc.getElementsByTagNameNS(NS_MAIN, "row");
        if (rowNodes.getLength() == 0) {
            rowNodes = doc.getElementsByTagName("row");
        }
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowEl = (Element) rowNodes.item(i);
            List<String> cells = new ArrayList<>();
            NodeList children = rowEl.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (!(node instanceof Element cellEl) || !"c".equals(localName(cellEl))) {
                    continue;
                }
                int col = columnIndex(cellEl.getAttribute("r"));
                while (cells.size() <= col) {
                    cells.add("");
                }
                cells.set(col, cellText(cellEl, shared));
            }
            rows.add(cells);
        }
        return rows;
    }

    private static String cellText(Element cell, List<String> shared) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return textOf(cell, "is");
        }
        String value = textOf(cell, "v");
        if ("s".equals(type)) {
            try {
                int idx = Integer.parseInt(value.trim());
                return idx >= 0 && idx < shared.size() ? shared.get(idx) : "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
        return value;
    }

    /** 取指定子元素（is/v）下所有 t 节点文本并拼接 */
    private static String textOf(Element cell, String childName) {
        NodeList children = cell.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element el && childName.equals(localName(el))) {
                NodeList ts = el.getElementsByTagNameNS(NS_MAIN, "t");
                if (ts.getLength() == 0) {
                    ts = el.getElementsByTagName("t");
                }
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < ts.getLength(); k++) {
                    sb.append(ts.item(k).getTextContent());
                }
                if (sb.length() > 0) {
                    return sb.toString();
                }
            }
        }
        return "";
    }

    private static List<String> parseSharedStrings(byte[] xml) {
        List<String> list = new ArrayList<>();
        if (xml == null) {
            return list;
        }
        try {
            Document doc = parse(xml);
            NodeList sis = doc.getElementsByTagNameNS(NS_MAIN, "si");
            if (sis.getLength() == 0) {
                sis = doc.getElementsByTagName("si");
            }
            for (int i = 0; i < sis.getLength(); i++) {
                Element si = (Element) sis.item(i);
                NodeList ts = si.getElementsByTagNameNS(NS_MAIN, "t");
                if (ts.getLength() == 0) {
                    ts = si.getElementsByTagName("t");
                }
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < ts.getLength(); k++) {
                    sb.append(ts.item(k).getTextContent());
                }
                list.add(sb.toString());
            }
        } catch (Exception e) {
            return list;
        }
        return list;
    }

    /** A1 -> 0, B2 -> 1 ... */
    private static int columnIndex(String ref) {
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                col = col * 26 + (c - 'A' + 1);
            } else {
                break;
            }
        }
        return Math.max(col - 1, 0);
    }

    private static Document parse(byte[] xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new IOException("解析 xlsx 内容失败: " + e.getMessage(), e);
        }
    }

    private static String localName(Node node) {
        String name = node.getNodeName();
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return XML_HEAD
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                + "</Types>";
    }

    private static String rootRels() {
        return XML_HEAD
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbook(String sheetName) {
        return XML_HEAD
                + "<workbook xmlns=\"" + NS_MAIN + "\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + escape(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String workbookRels() {
        return XML_HEAD
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private static String styles() {
        return XML_HEAD
                + "<styleSheet xmlns=\"" + NS_MAIN + "\">"
                + "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"宋体\"/></font></fonts>"
                + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>"
                + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
                + "<dxfs count=\"0\"/>"
                + "<tableStyles count=\"0\" defaultTableStyle=\"TableStyleMedium2\" defaultPivotStyle=\"PivotStyleLight16\"/>"
                + "</styleSheet>";
    }

    private static String sheetXml(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder(XML_HEAD);
        sb.append("<worksheet xmlns=\"").append(NS_MAIN).append("\"><sheetData>");
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            sb.append("<row r=\"").append(i + 1).append("\">");
            for (int j = 0; j < row.size(); j++) {
                String value = row.get(j);
                if (value == null || value.isEmpty()) {
                    continue;
                }
                sb.append("<c r=\"").append(columnName(j)).append(i + 1)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(escape(value)).append("</t></is></c>");
            }
            sb.append("</row>");
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static String columnName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
