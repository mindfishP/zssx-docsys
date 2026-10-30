package com.wechatapply.associationwechatapply.common.exception;

import com.wechatapply.associationwechatapply.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带错误码，由全局异常处理器统一转为 R 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BusinessException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }
}
