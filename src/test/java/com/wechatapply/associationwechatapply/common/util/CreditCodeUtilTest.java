package com.wechatapply.associationwechatapply.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一社会信用代码生成与校验（GB 32100-2015）。
 */
class CreditCodeUtilTest {

    @Test
    @DisplayName("生成结果：18 位、53 前缀（民政+社会团体+中山 442000）、字符集合法、校验码自洽")
    void generateValidCode() {
        String code = CreditCodeUtil.generate();
        assertEquals(18, code.length());
        assertTrue(code.startsWith("5144200"), code);
        assertTrue(CreditCodeUtil.isValid(code), "生成的代码应通过校验: " + code);
    }

    @Test
    @DisplayName("连续生成不重复（随机主体标识码）")
    void generateIsRandom() {
        String a = CreditCodeUtil.generate();
        String b = CreditCodeUtil.generate();
        assertFalse(a.equals(b), "两次生成不应相同");
    }

    @Test
    @DisplayName("校验码算法与标准样例一致：91350100M000100Y43")
    void checkCharMatchesStandardSample() {
        assertEquals('3', CreditCodeUtil.checkChar("91350100M000100Y4"));
        assertTrue(CreditCodeUtil.isValid("91350100M000100Y43"));
    }

    @Test
    @DisplayName("篡改任意一位后校验失败")
    void tamperedCodeIsInvalid() {
        String code = CreditCodeUtil.generate();
        char[] chars = code.toCharArray();
        chars[17] = chars[17] == '0' ? '1' : '0';   // 改最后一位校验码
        assertFalse(CreditCodeUtil.isValid(new String(chars)));
    }

    @Test
    @DisplayName("非法字符与长度不符时校验失败")
    void malformedCodeIsInvalid() {
        assertFalse(CreditCodeUtil.isValid("91350100M000100Y4"));    // 17 位
        assertFalse(CreditCodeUtil.isValid("91350100M000100Y44X"));  // 19 位
        assertFalse(CreditCodeUtil.isValid("91350100M000100Y4I"));   // 含非法字符 I
    }
}
