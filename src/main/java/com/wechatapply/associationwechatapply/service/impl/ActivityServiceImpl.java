package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.common.util.ActivityScoreCalculator;
import com.wechatapply.associationwechatapply.common.util.ActivityScoreCalculator.Metrics;
import com.wechatapply.associationwechatapply.common.util.ActivityScoreCalculator.Result;
import com.wechatapply.associationwechatapply.dto.ActivityResult;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizAssociation;
import com.wechatapply.associationwechatapply.entity.BizReview;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.mapper.BizArchiveMapper;
import com.wechatapply.associationwechatapply.mapper.BizAssociationMapper;
import com.wechatapply.associationwechatapply.mapper.BizReviewMapper;
import com.wechatapply.associationwechatapply.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 活跃度评估实现：一次性取出档案与审核记录后在内存聚合，避免逐协会查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    /** 会员类大类码 */
    private static final String MAJOR_MEMBER = "C";
    /** 活动类大类码 */
    private static final String MAJOR_ACTIVITY = "D";
    /** 年检年报材料分类编码（SRS 表 5-6） */
    private static final String CATEGORY_ANNUAL_CHECK = "A02";

    private final BizAssociationMapper assocMapper;
    private final BizArchiveMapper archiveMapper;
    private final BizReviewMapper reviewMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ActivityResult> evaluateAll() {
        List<ActivityResult> results = list();
        for (ActivityResult r : results) {
            BizAssociation update = new BizAssociation();
            update.setAssocCode(r.getAssocCode());
            update.setActivityScore(BigDecimal.valueOf(r.getScore()));
            update.setActivityLevel(r.getLevel());
            assocMapper.updateById(update);
        }
        log.info("活跃度评估完成，共 {} 个协会", results.size());
        return results;
    }

    @Override
    public List<ActivityResult> list() {
        String own = LoginContextHolder.getAssocCode();
        List<BizAssociation> assocs = assocMapper.selectList(Wrappers.<BizAssociation>lambdaQuery()
                .eq(own != null, BizAssociation::getAssocCode, own)
                .orderByAsc(BizAssociation::getAssocCode));
        if (assocs.isEmpty()) {
            return List.of();
        }
        List<BizArchive> archives = archiveMapper.selectList(Wrappers.<BizArchive>lambdaQuery());
        List<BizReview> reviews = reviewMapper.selectList(Wrappers.<BizReview>lambdaQuery());

        // bizId -> assocCode，用于把审核记录归到协会
        Map<Long, String> bizToAssoc = new HashMap<>();
        for (BizArchive a : archives) {
            if (a.getBizId() != null) {
                bizToAssoc.put(a.getBizId(), a.getAssocCode());
            }
        }
        // assocCode -> [审核次数, 退回次数]
        Map<String, long[]> reviewStats = new HashMap<>();
        for (BizReview r : reviews) {
            String code = bizToAssoc.get(r.getBizId());
            if (code == null) {
                continue;
            }
            long[] s = reviewStats.computeIfAbsent(code, k -> new long[2]);
            s[0]++;
            if (r.getReviewResult() != null && r.getReviewResult() == 2) {
                s[1]++;
            }
        }

        List<ActivityResult> results = new ArrayList<>();
        for (BizAssociation assoc : assocs) {
            String code = assoc.getAssocCode();
            List<BizArchive> own2 = archives.stream()
                    .filter(a -> code.equals(a.getAssocCode()))
                    .filter(a -> a.getStatus() != null && a.getStatus() == ArchiveServiceImpl.ST_ARCHIVED)
                    .toList();
            long memberArchives = own2.stream().filter(a -> MAJOR_MEMBER.equals(a.getMajorCode())).count();
            long activityArchives = own2.stream().filter(a -> MAJOR_ACTIVITY.equals(a.getMajorCode())).count();
            Set<String> majors = new HashSet<>();
            for (BizArchive a : own2) {
                if (a.getMajorCode() != null) {
                    majors.add(a.getMajorCode());
                }
            }
            boolean annual = own2.stream().anyMatch(a -> CATEGORY_ANNUAL_CHECK.equals(a.getCategoryCode()));

            long[] stat = reviewStats.getOrDefault(code, new long[2]);
            Metrics metrics = new Metrics(memberArchives, activityArchives, majors.size(), annual, stat[0], stat[1]);
            Result r = ActivityScoreCalculator.calculate(metrics);

            ActivityResult vo = new ActivityResult(code, assoc.getAssocName(), assoc.getIndustryType(),
                    r.score(), r.level(), ActivityScoreCalculator.levelText(r.level()),
                    r.memberIndex(), r.activityIndex(), r.coverageRate(), r.annualIndex(), r.passRate());
            results.add(vo);
        }
        return results;
    }
}
