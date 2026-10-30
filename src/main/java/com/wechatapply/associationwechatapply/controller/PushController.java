package com.wechatapply.associationwechatapply.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechatapply.associationwechatapply.common.exception.BusinessException;
import com.wechatapply.associationwechatapply.common.result.R;
import com.wechatapply.associationwechatapply.common.result.ResultCode;
import com.wechatapply.associationwechatapply.dto.PushPublishRequest;
import com.wechatapply.associationwechatapply.entity.BizAssociation;
import com.wechatapply.associationwechatapply.entity.BizPolicyPush;
import com.wechatapply.associationwechatapply.framework.annotation.AuditLog;
import com.wechatapply.associationwechatapply.framework.annotation.RequireRole;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.framework.context.RoleCode;
import com.wechatapply.associationwechatapply.mapper.BizAssociationMapper;
import com.wechatapply.associationwechatapply.mapper.BizPolicyPushMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 政策精准推送接口（SRS FR-09）。
 * 推送范围按“勾选具体协会 / 行业类别 / 活跃度分档”定向，协会端只看到命中本会的推送。
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final BizPolicyPushMapper pushMapper;
    private final BizAssociationMapper assocMapper;
    private final ObjectMapper objectMapper;

    /** 推送列表：协会角色只看到命中本会的已发布推送，主管部门/领导看全部 */
    @GetMapping("/page")
    public R<IPage<BizPolicyPush>> page(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "20") long size) {
        LambdaQueryWrapper<BizPolicyPush> query = Wrappers.lambdaQuery();
        if (RoleCode.ASSOC.equals(LoginContextHolder.getRoleCode())) {
            String assocCode = LoginContextHolder.getAssocCode();
            BizAssociation assoc = assocCode == null ? null : assocMapper.selectById(assocCode);
            String industry = assoc == null ? null : assoc.getIndustryType();
            String level = assoc == null || assoc.getActivityLevel() == null
                    ? null : String.valueOf(assoc.getActivityLevel());
            query.eq(BizPolicyPush::getStatus, 1).and(w -> {
                w.eq(BizPolicyPush::getTargetScope, "ALL");
                if (assocCode != null) {
                    w.or().apply("(target_scope = 'LIST' AND JSON_VALID(target_value) "
                            + "AND JSON_CONTAINS(target_value, JSON_QUOTE({0})))", assocCode);
                }
                if (industry != null) {
                    w.or().apply("(target_scope = 'INDUSTRY' AND JSON_VALID(target_value) "
                            + "AND JSON_CONTAINS(target_value, JSON_QUOTE({0})))", industry);
                }
                if (level != null) {
                    w.or().apply("(target_scope = 'LEVEL' AND JSON_VALID(target_value) "
                            + "AND JSON_CONTAINS(target_value, JSON_QUOTE({0})))", level);
                }
            });
        }
        query.orderByDesc(BizPolicyPush::getPushId);
        return R.ok(pushMapper.selectPage(Page.of(page, size), query));
    }

    /** 推送详情：查看文章正文；协会侧仅能查看命中本会的推送 */
    @GetMapping("/{pushId}")
    public R<BizPolicyPush> detail(@PathVariable Long pushId) {
        BizPolicyPush push = pushMapper.selectById(pushId);
        if (push == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "推送不存在");
        }
        if (RoleCode.ASSOC.equals(LoginContextHolder.getRoleCode()) && !visibleTo(push)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该推送不在本会接收范围内");
        }
        return R.ok(push);
    }

    /** 协会是否命中推送范围 */
    private boolean visibleTo(BizPolicyPush push) {
        if (push.getStatus() == null || push.getStatus() != 1) {
            return false;
        }
        if ("ALL".equals(push.getTargetScope())) {
            return true;
        }
        String assocCode = LoginContextHolder.getAssocCode();
        BizAssociation assoc = assocCode == null ? null : assocMapper.selectById(assocCode);
        List<String> targets = parseTargets(push.getTargetValue());
        return switch (push.getTargetScope() == null ? "" : push.getTargetScope()) {
            case "LIST" -> assocCode != null && targets.contains(assocCode);
            case "INDUSTRY" -> assoc != null && targets.contains(assoc.getIndustryType());
            case "LEVEL" -> assoc != null && assoc.getActivityLevel() != null
                    && targets.contains(String.valueOf(assoc.getActivityLevel()));
            default -> false;
        };
    }

    private List<String> parseTargets(String json) {
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() { });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }
    /** 发布推送（仅主管部门操作员）：范围参数由前端勾选，数量不限（SRS FR-09-02） */
    @PostMapping
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "发布政策推送")
    public R<BizPolicyPush> publish(@Valid @RequestBody PushPublishRequest req) {
        String scope = req.getTargetScope().toUpperCase();
        if (!"ALL".equals(scope) && !"LIST".equals(scope)
                && !"INDUSTRY".equals(scope) && !"LEVEL".equals(scope)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "推送范围不合法");
        }
        List<String> targets = normalizeTargets(req.getTargets());
        if (!"ALL".equals(scope) && targets.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择推送对象");
        }
        if ("LIST".equals(scope)) {
            // 校验协会编号存在，避免推送对象写成无效值
            for (String code : targets) {
                if (assocMapper.selectById(code) == null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "协会不存在：" + code);
                }
            }
        }

        BizPolicyPush push = new BizPolicyPush();
        push.setTitle(req.getTitle());
        push.setContent(req.getContent());
        push.setAttachPath(req.getAttachPath());
        push.setTargetScope(scope);
        push.setTargetValue(toJson(targets));
        push.setCreatedBy(String.valueOf(LoginContextHolder.getUserId()));
        push.setStatus(1);
        push.setPublishedAt(LocalDateTime.now());
        pushMapper.insert(push);
        // TODO 订阅消息通道：调用微信订阅消息接口完成双通道送达并登记回执（SRS FR-09-03）
        return R.ok(push);
    }

    /** 撤回推送 */
    @PostMapping("/{pushId}/withdraw")
    @RequireRole(RoleCode.ADMIN)
    @AuditLog(action = "撤回政策推送")
    public R<Void> withdraw(@PathVariable Long pushId) {
        BizPolicyPush p = pushMapper.selectById(pushId);
        if (p != null) {
            p.setStatus(2);
            pushMapper.updateById(p);
        }
        return R.ok();
    }

    /** 去重并剔除空值：勾选数量不设上限 */
    private List<String> normalizeTargets(List<String> targets) {
        Set<String> distinct = new LinkedHashSet<>();
        if (targets != null) {
            for (String t : targets) {
                if (t != null && !t.isBlank()) {
                    distinct.add(t.trim());
                }
            }
        }
        return new ArrayList<>(distinct);
    }

    private String toJson(List<String> targets) {
        try {
            return objectMapper.writeValueAsString(targets);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "推送范围参数格式错误");
        }
    }
}
