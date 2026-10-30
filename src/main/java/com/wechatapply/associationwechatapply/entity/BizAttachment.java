package com.wechatapply.associationwechatapply.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件（SRS 表 5-3 biz_attachment）：内容随数据库保存，配合 SHA-256 固证与时效签名访问。
 * 内容存库可保证数据库迁移/换机后附件依然可打开，不依赖服务器本地目录。
 */
@Data
@TableName("biz_attachment")
public class BizAttachment {

    @TableId(type = IdType.AUTO)
    private Long attachId;

    /** 所属档案条目内部编号 */
    private Long bizId;

    private String fileName;

    /** 旧版磁盘相对路径（历史数据兼容，新上传的附件为 null） */
    private String storePath;

    /** 附件二进制内容（随数据库迁移；接口返回时忽略，避免列表接口传输大字段） */
    @JsonIgnore
    private byte[] fileContent;

    /** SHA-256 哈希值（归档固证依据，SRS 5.4） */
    private String fileHash;
    private String fileType;
    private Long fileSize;

    private String uploadBy;
    private LocalDateTime uploadAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
