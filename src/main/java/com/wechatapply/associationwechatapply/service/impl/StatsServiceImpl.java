package com.wechatapply.associationwechatapply.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import com.wechatapply.associationwechatapply.entity.BizAssociation;
import com.wechatapply.associationwechatapply.framework.context.LoginContextHolder;
import com.wechatapply.associationwechatapply.mapper.BizArchiveMapper;
import com.wechatapply.associationwechatapply.mapper.BizAssociationMapper;
import com.wechatapply.associationwechatapply.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final BizAssociationMapper assocMapper;
    private final BizArchiveMapper archiveMapper;

    @Override
    public Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        // 商协会管理员仅看本会数据（SRS 4.1 权限矩阵）
        String own = LoginContextHolder.getAssocCode();

        List<BizAssociation> assocs = assocMapper.selectList(Wrappers.<BizAssociation>lambdaQuery()
                .eq(own != null, BizAssociation::getAssocCode, own));
        data.put("assocTotal", assocs.size());

        Map<String, Long> industryDist = new LinkedHashMap<>();
        assocs.forEach(a -> industryDist.merge(
                a.getIndustryType() == null ? "其他" : a.getIndustryType(), 1L, Long::sum));
        data.put("industryDistribution", industryDist);

        List<BizArchive> archives = archiveMapper.selectList(Wrappers.<BizArchive>lambdaQuery()
                .eq(own != null, BizArchive::getAssocCode, own));
        data.put("archiveTotal", archives.size());

        Map<String, Long> majorDist = new LinkedHashMap<>();
        for (String major : new String[]{"A", "B", "C", "D", "E", "F"}) {
            majorDist.put(major, archives.stream()
                    .filter(x -> major.equals(x.getMajorCode()))
                    .filter(x -> x.getStatus() != null && x.getStatus() == ArchiveServiceImpl.ST_ARCHIVED)
                    .count());
        }
        data.put("majorDistribution", majorDist);

        Map<String, Long> levelDist = new LinkedHashMap<>();
        levelDist.put("示范型", assocs.stream().filter(a -> a.getActivityLevel() != null && a.getActivityLevel() == 1).count());
        levelDist.put("成长型", assocs.stream().filter(a -> a.getActivityLevel() != null && a.getActivityLevel() == 2).count());
        levelDist.put("休眠型", assocs.stream().filter(a -> a.getActivityLevel() != null && a.getActivityLevel() == 3).count());
        data.put("activityLevels", levelDist);

        return data;
    }
}
