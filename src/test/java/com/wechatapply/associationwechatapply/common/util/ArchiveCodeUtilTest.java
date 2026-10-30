package com.wechatapply.associationwechatapply.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 档案唯一标识生成规则单元测试（SRS 5.3）。
 */
class ArchiveCodeUtilTest {

    @Test
    void shouldMatchSpecExample() {
        // 示例：ZSA-A012-B03-2026-000137（SRS 5.3）
        String code = ArchiveCodeUtil.nextCode("A012", "B", "03", "2026", 137L);
        assertEquals("ZSA-A012-B03-2026-000137", code);
    }

    @Test
    void shouldPadSeqToSixDigits() {
        assertEquals("ZSA-A001-A01-2026-000001", ArchiveCodeUtil.nextCode("A001", "A", "01", "2026", 1L));
        assertEquals("ZSA-A001-F01-2025-099999", ArchiveCodeUtil.nextCode("A001", "F", "01", "2025", 99999L));
    }

    @Test
    void shouldRejectBlankInputs() {
        assertThrows(Exception.class, () -> ArchiveCodeUtil.nextCode("", "B", "03", "2026", 1L));
    }
}
