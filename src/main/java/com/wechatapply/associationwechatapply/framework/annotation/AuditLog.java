package com.wechatapply.associationwechatapply.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解（SRS FR-10-01/02）。
 * 标注在 Controller 方法上，由 AuditLogAspect 切面记录操作人、动作、对象与结果。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 操作动作描述，如"档案审核" */
    String action();
}
