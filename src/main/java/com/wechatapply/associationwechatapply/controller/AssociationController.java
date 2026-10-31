package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.common.util.CreditCodeUtil;
import com.wechatapply.associationwechatapply.entity.BizAssociation;
import com.wechatapply.associationwechatapply.entity.SysUser;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.BizAssociationMapper;
import com.wechatapply.associationwechatapply.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商协会主档接口（SRS FR-02）。
 */
@RestController
@RequestMapping("/api/association")
@RequiredArgsConstructor
public class AssociationController {

    private final BizAssociationMapper assocMapper;
    private final SysUserMapper userMapper;

    /** 名录列表：全局角色可看全部，协会角色仅本会 */
    @GetMapping("/page")
    public R<IPage<BizAssociation>> page(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String industryType,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(assocMapper.selectPage(Page.of(page, size), Wrappers.<BizAssociation>lambdaQuery()
                .eq(LoginContextHolder.getAssocCode() != null,
                        BizAssociation::getAssocCode, LoginContextHolder.getAssocCode())
                .eq(industryType != null && !industryType.isBlank(), BizAssociation::getIndustryType, industryType)
                .like(keyword != null && !keyword.isBlank(), BizAssociation::getAssocName, keyword)
                .orderByAsc(BizAssociation::getAssocCode)));
    }

    /** 新增协会主档（仅主管部门操作员）：统一社会信用代码由系统按 GB 32100-2015 自动生成 */
    @PostMapping
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "新增商协会")
    public R<BizAssociation> create(@RequestBody BizAssociation assoc) {
        if (assoc.getAssocCode() == null || assoc.getAssocCode().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "协会编号不能为空");
        }
        if (assocMapper.selectById(assoc.getAssocCode()) != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "协会编号已存在：" + assoc.getAssocCode());
        }
        if (assoc.getAssocName() == null || assoc.getAssocName().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "协会名称不能为空");
        }
        // 未填写时自动生成（沿用旧接口传入的值则保留，便于数据迁移）
        if (assoc.getCreditCode() == null || assoc.getCreditCode().isBlank()) {
            assoc.setCreditCode(nextCreditCode());
        }
        if (assoc.getStatus() == null) {
            assoc.setStatus(1);
        }
        assocMapper.insert(assoc);
        return R.ok(assoc);
    }

    /** 生成库内不重复的统一社会信用代码 */
    private String nextCreditCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = CreditCodeUtil.generate();
            Long exists = assocMapper.selectCount(Wrappers.<BizAssociation>lambdaQuery()
                    .eq(BizAssociation::getCreditCode, code));
            if (exists == null || exists == 0) {
                return code;
            }
        }
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "统一社会信用代码生成失败，请重试");
    }

    /** 协会信息维护：主管部门全局改，协会仅本会部分字段 */
    @PutMapping("/{assocCode}")
    @RequireRole({RoleCode.ADMIN, RoleCode.ASSOC})
    @AuditLog(action = "维护商协会信息")
    public R<Void> update(@PathVariable String assocCode, @RequestBody BizAssociation assoc) {
        // 行级隔离：协会角色禁止改他协会（SRS 4.2 规则 1）
        String own = LoginContextHolder.getAssocCode();
        if (own != null && !own.equals(assocCode)) {
            return R.fail(403, "仅可维护本协会信息");
        }
        if (assocMapper.selectById(assocCode) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "协会不存在");
        }
        // 字段白名单：信用代码、活跃度、创建时间等由系统维护，不接受外部提交；
        // 登记信息与状态仅主管部门可改（SRS FR-02 权限矩阵）
        BizAssociation update = new BizAssociation();
        update.setAssocCode(assocCode);
        update.setAssocName(assoc.getAssocName());
        update.setIndustryType(assoc.getIndustryType());
        update.setLegalPerson(assoc.getLegalPerson());
        update.setContactPhone(assoc.getContactPhone());
        if (own == null) {
            update.setRegisterOrg(assoc.getRegisterOrg());
            update.setRegisterDate(assoc.getRegisterDate());
            update.setStatus(assoc.getStatus());
        }
        assocMapper.updateById(update);
        return R.ok();
    }

    /** 协会详情（名录详情页） */
    @GetMapping("/{assocCode}")
    @RequireRole({RoleCode.ADMIN, RoleCode.LEADER})
    public R<BizAssociation> detail(@PathVariable String assocCode) {
        BizAssociation assoc = assocMapper.selectById(assocCode);
        if (assoc == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "协会不存在");
        }
        return R.ok(assoc);
    }

    /** 该协会下的全部账号（名录详情页展示成员账号） */
    @GetMapping("/{assocCode}/users")
    @RequireRole({RoleCode.ADMIN, RoleCode.LEADER})
    public R<List<SysUser>> users(@PathVariable String assocCode) {
        return R.ok(userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getAssocCode, assocCode)
                .orderByAsc(SysUser::getId)));
    }

    /**
     * 全部协会（下拉框用）。
     * 协会角色只返回编号/名称/行业等公开字段，避免看到其他协会的信用代码、法人、联系电话等登记信息；
     * 完整信息仅主管部门与主管领导可见。
     */
    @GetMapping("/all")
    public R<List<BizAssociation>> all() {
        List<BizAssociation> list = assocMapper.selectList(Wrappers.<BizAssociation>lambdaQuery()
                .orderByAsc(BizAssociation::getAssocCode));
        if (LoginContextHolder.getAssocCode() != null) {
            for (BizAssociation a : list) {
                a.setCreditCode(null);
                a.setLegalPerson(null);
                a.setContactPhone(null);
                a.setRegisterOrg(null);
                a.setRegisterDate(null);
                a.setActivityScore(null);
                a.setActivityLevel(null);
                a.setCreatedAt(null);
                a.setUpdatedAt(null);
            }
        }
        return R.ok(list);
    }
}
