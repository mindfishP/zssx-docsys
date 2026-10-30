package com.wechatapply.associationwechatapply.common.util;

import java.security.SecureRandom;

/**
 * 统一社会信用代码生成（GB 32100-2015）。
 * 结构 18 位：登记管理部门码(1) + 机构类别码(1) + 行政区划码(6) + 主体标识码(9) + 校验码(1)。
 * 社会团体登记管理部门为民政部门（5），机构类别为社会团体（1），因此前缀通常为 51。
 */
public final class CreditCodeUtil {

    /** 代码字符集：不包含 I、O、S、V、Z */
    private static final String CHARSET = "0123456789ABCDEFGHJKLMNPQRTUWXY";

    /** 前 17 位的加权因子 */
    private static final int[] WEIGHTS = {
            1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28
    };

    /** 民政部门 + 社会团体 */
    private static final String PREFIX = "51";

    /** 中山市行政区划代码 */
    public static final String ZHONGSHAN_DIVISION = "442000";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CreditCodeUtil() {
    }

    /** 生成中山市社会团体统一社会信用代码 */
    public static String generate() {
        return generate(ZHONGSHAN_DIVISION);
    }

    /**
     * @param divisionCode 6 位行政区划代码
     */
    public static String generate(String divisionCode) {
        if (divisionCode == null || divisionCode.length() != 6) {
            throw new IllegalArgumentException("行政区划代码必须为 6 位");
        }
        StringBuilder sb = new StringBuilder(18);
        sb.append(PREFIX).append(divisionCode);
        for (int i = 0; i < 9; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        sb.append(checkChar(sb));
        return sb.toString();
    }

    /** 按 GB 32100-2015 计算校验码（输入前 17 位） */
    public static char checkChar(CharSequence body17) {
        if (body17 == null || body17.length() != 17) {
            throw new IllegalArgumentException("需传入 17 位本体码");
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int value = CHARSET.indexOf(Character.toUpperCase(body17.charAt(i)));
            if (value < 0) {
                throw new IllegalArgumentException("统一社会信用代码含非法字符: " + body17.charAt(i));
            }
            sum += value * WEIGHTS[i];
        }
        int check = 31 - sum % 31;
        if (check == 31) {
            check = 0;
        }
        return CHARSET.charAt(check);
    }

    /** 校验 18 位统一社会信用代码是否合法 */
    public static boolean isValid(String code) {
        if (code == null || code.length() != 18) {
            return false;
        }
        String upper = code.toUpperCase();
        for (int i = 0; i < 18; i++) {
            if (CHARSET.indexOf(upper.charAt(i)) < 0) {
                return false;
            }
        }
        return checkChar(upper.substring(0, 17)) == upper.charAt(17);
    }
}
