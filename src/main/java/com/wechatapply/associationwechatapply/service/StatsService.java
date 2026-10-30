package com.wechatapply.associationwechatapply.service;

import java.util.Map;

/**
 * 统计与活跃度评估服务（SRS FR-08）。框架阶段提供基础汇总，图表缓存后续接入。
 */
public interface StatsService {

    /** 主看板：协会总数、各行业分布、六大类归档数量、填报完成率、活跃度分档统计 */
    Map<String, Object> dashboard();
}
