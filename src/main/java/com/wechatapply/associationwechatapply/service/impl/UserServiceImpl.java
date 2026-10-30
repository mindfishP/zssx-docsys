package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.dto.ChangePasswordRequest;
import com.wechatapply.associationwechatapply.entity.SysUser;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.SysUserMapper;
import com.wechatapply.associationwechatapply.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 账号管理实现（SRS FR-01）。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void changePassword(ChangePasswordRequest req) {
        Long userId = LoginContextHolder.getUserId();
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前账号不存在");
        }
        if (!encoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }
        user.setPasswordHash(encoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void updateMyName(String realName) {
        Long userId = LoginContextHolder.getUserId();
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前账号不存在");
        }
        user.setRealName(normalizeName(realName));
        userMapper.updateById(user);
    }

    @Override
    public void updateName(Long userId, String realName) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号不存在或已删除");
        }
        user.setRealName(normalizeName(realName));
        userMapper.updateById(user);
    }

    private String normalizeName(String realName) {
        if (realName == null || realName.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "姓名不能为空");
        }
        String name = realName.trim();
        if (name.length() > 32) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "姓名不超过 32 个字符");
        }
        return name;
    }

    @Override
    public IPage<SysUser> page(Page<SysUser> page, String keyword, String roleCode) {
        return userMapper.selectPage(page, Wrappers.<SysUser>lambdaQuery()
                .and(StringUtils.isNotBlank(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword))
                .eq(StringUtils.isNotBlank(roleCode), SysUser::getRoleCode, roleCode)
                .orderByAsc(SysUser::getId));
    }

    @Override
    public void delete(Long userId) {
        SysUser target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号不存在或已删除");
        }
        if (target.getId().equals(LoginContextHolder.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能删除当前登录的账号");
        }
        if (RoleCode.ADMIN.equals(target.getRoleCode())) {
            long admins = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getRoleCode, RoleCode.ADMIN));
            if (admins <= 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "系统至少保留一个管理员账号");
            }
        }
        // 逻辑删除：保留账号记录便于审计追溯
        userMapper.deleteById(userId);
    }
}
