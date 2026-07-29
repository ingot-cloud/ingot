package com.ingot.framework.security.account.adapter.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全事件实体（基础类）
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@TableName("account_security_event")
public class AccountSecurityEventEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件分类
     */
    private String eventCategory;

    /**
     * 原因代码
     */
    private String reasonCode;

    /**
     * 原因详情
     */
    private String reasonDetail;

    /**
     * 结果
     */
    private String result;

    /**
     * 来源
     */
    private String source;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 客户端信息
     */
    private String userAgent;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 扩展数据（JSON）
     */
    private String extraData;

    /**
     * 事件时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
