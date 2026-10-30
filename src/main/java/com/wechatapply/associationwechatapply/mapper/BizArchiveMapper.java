package com.wechatapply.associationwechatapply.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wechatapply.associationwechatapply.entity.BizArchive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BizArchiveMapper extends BaseMapper<BizArchive> {

    /**
     * 取指定编号前缀下已用的最大流水号。
     * 这里显式写成自定义 SQL，绕开逻辑删除条件：已删除档案的编号仍占用唯一索引，
     * 若只统计未删除记录会生成重复档号（曾导致“通过并归档”报唯一键冲突）。
     *
     * @param prefix 形如 ZSA-A001-A01-2026-
     * @return 已用最大流水号，没有记录时返回 0
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(archive_id, '-', -1) AS UNSIGNED)), 0) "
            + "FROM biz_archive WHERE archive_id LIKE CONCAT(#{prefix}, '%')")
    long maxArchiveSeq(@Param("prefix") String prefix);
}
