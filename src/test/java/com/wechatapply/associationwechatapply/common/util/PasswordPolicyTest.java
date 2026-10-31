package com.wechatapply.associationwechatapply.common.util;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 口令强度策略（SRS FR-01）：8-32 位且同时包含字母与数字。
 */
class PasswordPolicyTest {

    @Test
    @DisplayName("合法口令：8 位以上、字母与数字混合")
    void acceptsValid() {
        assertTrue(PasswordPolicy.isValid("Zssx2026"));
        assertTrue(PasswordPolicy.isValid("a1234567"));
        assertTrue(PasswordPolicy.isValid("PASSWORD2026abc"));
    }

    @Test
    @DisplayName("长度不足 8 位被拒绝")
    void rejectsShort() {
        BusinessException ex = assertThrows(BusinessException.class, () -> PasswordPolicy.check("abc123"));
        assertTrue(ex.getMessage().contains("至少 8 位"));
        assertFalse(PasswordPolicy.isValid("Ab1"));
    }

    @Test
    @DisplayName("纯数字或纯字母被拒绝")
    void rejectsSingleCharClass() {
        BusinessException noLetter = assertThrows(BusinessException.class, () -> PasswordPolicy.check("12345678"));
        assertTrue(noLetter.getMessage().contains("字母和数字"));

        BusinessException noDigit = assertThrows(BusinessException.class, () -> PasswordPolicy.check("abcdefgh"));
        assertTrue(noDigit.getMessage().contains("字母和数字"));
    }

    @Test
    @DisplayName("超过 32 位被拒绝")
    void rejectsTooLong() {
        String tooLong = "Ab1" + "x".repeat(40);
        BusinessException ex = assertThrows(BusinessException.class, () -> PasswordPolicy.check(tooLong));
        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    @DisplayName("空口令被拒绝")
    void rejectsNull() {
        assertThrows(BusinessException.class, () -> PasswordPolicy.check(null));
        assertFalse(PasswordPolicy.isValid(""));
    }
}
