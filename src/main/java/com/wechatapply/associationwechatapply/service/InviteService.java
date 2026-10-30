package com.wechatapply.associationwechatapply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.dto.CreateInviteRequest;
import com.wechatapply.associationwechatapply.entity.SysInviteCode;

/**
 * 注册邀请码服务（SRS FR-01）：主管部门发放，注册时校验并核销。
 */
public interface InviteService {

    /** 创建邀请码：绑定角色与协会，有效期 12 小时 */
    SysInviteCode create(CreateInviteRequest req);

    /** 邀请码分页（仅主管部门操作员） */
    IPage<SysInviteCode> page(Page<SysInviteCode> page, Integer status);
}
