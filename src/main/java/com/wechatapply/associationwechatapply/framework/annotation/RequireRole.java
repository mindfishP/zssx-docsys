package com.wechatapply.associationwechatapply.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色鉴权注解（RBAC，SRS 4.2）。
 * 标注在 Controller 方法或类上；value 为允许访问的角色编码集合。
 * 角色编码见枚举 RoleCode：ADMIN（主管部门操作员）/ LEADER（主管领导只读）/ ASSOC（商协会管理员）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    String[] value() default {};
}
