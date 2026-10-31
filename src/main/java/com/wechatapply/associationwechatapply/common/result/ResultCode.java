package com.wechatapply.associationwechatapply.common.result;

import lombok.Getter;

/**
 * 业务错误码（分段定义见 SRS 7.1）。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或令牌已过期"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "资源不存在"),

    // 1xxx 用户与权限模块
    USER_LOGIN_FAILED(1001, "账号或口令错误"),
    USER_LOCKED(1002, "账号已被锁定，请稍后再试"),
    USER_DISABLED(1003, "账号已停用"),
    WECHAT_LOGIN_FAILED(1004, "微信登录失败"),
    USER_EXISTS(1005, "该账号已被占用"),
    INVITE_INVALID(1006, "邀请码无效"),
    INVITE_USED(1007, "邀请码已被使用"),
    INVITE_EXPIRED(1008, "邀请码已过期（有效期 12 小时）"),

    // 3xxx 档案模块
    ARCHIVE_NOT_FOUND(3001, "档案条目不存在"),
    ARCHIVE_STATUS_INVALID(3002, "档案条目当前状态不允许该操作"),
    ARCHIVE_METADATA_INCOMPLETE(3003, "必填元数据不完整"),
    ATTACHMENT_TYPE_INVALID(3004, "附件格式不支持"),
    ATTACHMENT_TOO_LARGE(3005, "附件超出大小限制"),
    ATTACHMENT_UPLOAD_FAILED(3006, "附件上传失败"),

    // 5xxx 借阅模块
    BORROW_NOT_ALLOWED(5001, "该档案不可借阅"),
    BORROW_STATUS_INVALID(5002, "借阅记录状态不允许该操作"),

    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    SYSTEM_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
