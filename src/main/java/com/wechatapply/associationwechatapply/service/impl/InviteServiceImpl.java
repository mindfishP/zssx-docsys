package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.dto.CreateInviteRequest;
import com.wechatapply.associationwechatapply.entity.SysInviteCode;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.BizAssociationMapper;
import com.wechatapply.associationwechatapply.mapper.SysInviteCodeMapper;
import com.wechatapply.associationwechatapply.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 邀请码实现：8 位随机码（去掉易混淆字符），12 小时有效。
 */
@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements InviteService {

    /** 有效期：12 小时 */
    public static final int VALID_HOURS = 12;

    /** 去掉 0/O/1/I/L 等易混淆字符 */
    private static final char[] ALPHABET =
            "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private final SysInviteCodeMapper inviteMapper;
    private final BizAssociationMapper assocMapper;
    private final SecureRandom random = new SecureRandom();

    @Override
    public SysInviteCode create(CreateInviteRequest req) {
        String role = req.getRoleCode() == null ? "" : req.getRoleCode().toUpperCase();
        if (!RoleCode.ADMIN.equals(role) && !RoleCode.LEADER.equals(role) && !RoleCode.ASSOC.equals(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色不合法");
        }
        String assocCode = req.getAssocCode() == null ? null : req.getAssocCode().trim();
        if (RoleCode.ASSOC.equals(role)) {
            if (StringUtils.isBlank(assocCode)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "商协会管理员需指定加入的协会");
            }
            if (assocMapper.selectById(assocCode) == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "协会不存在：" + assocCode);
            }
        } else {
            assocCode = null;   // 主管部门与领导不绑定协会
        }

        SysInviteCode invite = new SysInviteCode();
        invite.setCode(nextUniqueCode());
        invite.setRoleCode(role);
        invite.setAssocCode(assocCode);
        invite.setStatus(0);
        invite.setExpiresAt(LocalDateTime.now().plusHours(VALID_HOURS));
        invite.setCreatedBy(String.valueOf(LoginContextHolder.getUserId()));
        inviteMapper.insert(invite);
        return invite;
    }

    @Override
    public IPage<SysInviteCode> page(Page<SysInviteCode> page, Integer status) {
        return inviteMapper.selectPage(page, Wrappers.<SysInviteCode>lambdaQuery()
                .eq(status != null, SysInviteCode::getStatus, status)
                .orderByDesc(SysInviteCode::getInviteId));
    }

    /** 生成不重复的 8 位邀请码 */
    private String nextUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            String code = sb.toString();
            Long exists = inviteMapper.selectCount(Wrappers.<SysInviteCode>lambdaQuery()
                    .eq(SysInviteCode::getCode, code));
            if (exists == null || exists == 0) {
                return code;
            }
        }
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "邀请码生成失败，请重试");
    }
}
