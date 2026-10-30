package com.wechatapply.associationwechatapply.framework.context;

/**
 * 预置角色编码（SRS 2.1 三类角色）。
 */
public final class RoleCode {

    /** 主管部门操作员：全局管理 */
    public static final String ADMIN = "ADMIN";
    /** 主管领导：全局只读 */
    public static final String LEADER = "LEADER";
    /** 商协会管理员：仅本会数据 */
    public static final String ASSOC = "ASSOC";

    private RoleCode() {
    }
}
