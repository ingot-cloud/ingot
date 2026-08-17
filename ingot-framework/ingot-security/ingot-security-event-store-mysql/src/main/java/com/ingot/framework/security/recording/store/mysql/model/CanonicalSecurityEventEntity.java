package com.ingot.framework.security.recording.store.mysql.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>canonical {@code security_event} 表实体，供 recording MySQL Store 读写。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@TableName("security_event")
public class CanonicalSecurityEventEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;
    private String eventType;
    private String eventCategory;
    private String priority;
    private LocalDateTime occurredAt;
    private LocalDateTime receivedAt;

    private Long tenantId;
    private Long userId;
    private String userType;
    private String account;

    private String clientId;
    private String appId;
    private String sessionId;
    private String deviceId;

    private String clientIp;
    private String requestUri;
    private String userAgent;

    private String result;
    private String reasonCode;
    private String reasonDetail;

    private String sourceModule;
    private String source;

    private Long operatorId;
    private String operatorName;
    private String traceId;

    @TableField("extension")
    private String extensionJson;
}
