package com.wechatapply.associationwechatapply.common.util;

/**
 * 档案唯一标识生成（SRS 5.3）：
 * ZSA-协会编号-大类码中类码-年度-流水号，示例 ZSA-A012-B03-2026-000137。
 */
public final class ArchiveCodeUtil {

    public static final String PREFIX = "ZSA";

    private ArchiveCodeUtil() {
    }

    /**
     * @param assocCode 协会编号，如 A012
     * @param majorCode 大类码 A-F
     * @param subCode   中类码，两位数字，如 03
     * @param year      归档年度
     * @param seq       该协会同年同中类下的顺序号
     */
    public static String nextCode(String assocCode, String majorCode, String subCode, String year, long seq) {
        if (isBlank(assocCode) || isBlank(majorCode) || isBlank(subCode) || isBlank(year)) {
            throw new IllegalArgumentException("档案编码各段参数不能为空");
        }
        return String.format("%s-%s-%s%s-%s-%06d", PREFIX, assocCode, majorCode, subCode, year, seq);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
