package com.wechatapply.associationwechatapply.common.util;

import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;

import java.util.regex.Pattern;

/**
 * 口令强度策略（SRS FR-01 安全要求）：
 * 长度 8-32 位，且同时包含字母与数字；注册与修改口令都必须通过校验。
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;

    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    private PasswordPolicy() {
    }

    /** 校验口令强度，不合规则抛出 BusinessException（错误码 400） */
    public static void check(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "密码至少 " + MIN_LENGTH + " 位");
        }
        if (password.length() > MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "密码不能超过 " + MAX_LENGTH + " 位");
        }
        if (!LETTER.matcher(password).find() || !DIGIT.matcher(password).find()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码需同时包含字母和数字");
        }
    }

    /** 是否满足强度要求（供前端提示或批量校验使用） */
    public static boolean isValid(String password) {
        try {
            check(password);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}