package com.wechatapply.associationwechatapply.service;

import com.wechatapply.associationwechatapply.dto.ActivityResult;

import java.util.List;

/**
 * 活跃度评估服务（SRS FR-08 / 表 3-11）：
 * 从档案数据计算会员规模、活动频次、填报完整率、年检达标、审核一次通过率，加权得到百分制得分并分档。
 */
public interface ActivityService {

    /** 计算全部协会的活跃度并写回商协会主档（主管部门手动触发或定时任务调用） */
    List<ActivityResult> evaluateAll();

    /** 仅计算不落库，用于看板展示（商协会角色只返回本会） */
    List<ActivityResult> list();
}
