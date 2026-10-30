package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案分类（SRS 表 5-5 biz_category，六大类两级分类树）。
 */
@Data
@TableName("biz_category")
public class BizCategory {

    /** 分类编码，如 B03 = 治理类第 3 中类 */
    @TableId(type = IdType.INPUT)
    private String categoryCode;

    /** 大类码 A-F（SRS 表 5-6） */
    private String majorCode;
    /** 大类名称：登记类/治理类/会员类/活动类/财务类/党建类 */
    private String majorName;

    /** 中类码，两位数字 */
    private String subCode;
    private String subName;

    /** 默认保管期限：永久 / 定期30年 / 定期10年 */
    private String defaultRetention;
    /** 默认密级：1 公开，2 内部 */
    private Integer defaultSecurityLevel;

    /** 必填元数据项编码集合，JSON 数组（元数据字典驱动表单，SRS FR-03-02） */
    private String requiredMetaKeys;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
