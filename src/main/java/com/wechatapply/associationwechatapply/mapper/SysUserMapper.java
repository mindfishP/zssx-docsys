package com.wechatapply.associationwechatapply.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wechatapply.associationwechatapply.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按账号统计用户数（包含逻辑删除的记录）。
     * 用户名上有唯一索引，已删除账号仍占用该用户名，注册时必须一并判断，
     * 否则会出现“业务校验通过、插入时报唯一键冲突”的情况。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    long countByUsername(@Param("username") String username);
}
