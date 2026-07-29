package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一安全事件实体，映射 {@code security_event}。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "security_event", autoResultMap = true)
public class SecurityEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventType;
    private String eventCategory;
    private LocalDateTime occurredAt;

    @TableField(fill = FieldFill.INSERT)
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

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extension;
}
