package com.wechatapply.associationwechatapply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.dto.ChangePasswordRequest;
import com.wechatapply.associationwechatapply.entity.SysUser;

/**
 * 账号管理服务（SRS FR-01）：本人改密 + 主管部门账号维护。
 */
public interface UserService {

    /** 修改当前登录用户的密码 */
    void changePassword(ChangePasswordRequest req);

    /** 修改当前登录用户的姓名（账号与姓名分开维护） */
    void updateMyName(String realName);

    /** 主管部门修改指定账号的姓名 */
    void updateName(Long userId, String realName);

    /** 账号分页（仅主管部门操作员）：按账号/姓名模糊查询，可按角色过滤 */
    IPage<SysUser> page(Page<SysUser> page, String keyword, String roleCode);

    /** 删除账号（仅主管部门操作员，逻辑删除，不能删自己与最后一个管理员） */
    void delete(Long userId);
}
