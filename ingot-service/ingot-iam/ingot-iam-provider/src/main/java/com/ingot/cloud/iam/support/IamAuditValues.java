package com.ingot.cloud.iam.support;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * <p>统一审计白名单差异的字符串契约，兼容历史结构值，不把内部格式损坏归为请求错误。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public final class IamAuditValues {
    /** 变更前审计列名，用于安全诊断。 */
    public static final String BEFORE_COLUMN = "safe_before";
    /** 变更后审计列名，用于安全诊断。 */
    public static final String AFTER_COLUMN = "safe_after";
    /** 版本关联列名，用于安全诊断。 */
    public static final String REVISIONS_COLUMN = "revisions";
    private static final String TRACE_ID = "traceId";
    private static final String READ_FAILURE = "暂时无法读取授权审计，请稍后重试";
    private static final String WRITE_FAILURE = "暂时无法保存授权审计，请稍后重试";
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private IamAuditValues() {
    }

    /**
     * 将调用方已脱敏的白名单差异标准化为字符串后序列化，保持业务事务失败回滚。
     * @param auditId 审计 ID
     * @param column 安全列名
     * @param fields 安全差异，null 作为空对象
     * @return JSON 对象文本；结构值保留为紧凑 JSON 字符串
     */
    public static String writeFields(long auditId, String column, Map<AuditField, ?> fields) {
        try {
            Map<AuditField, String> result = new LinkedHashMap<>();
            if (fields != null) {
                for (var field : fields.entrySet()) {
                    if (field.getKey() == null) {
                        throw new IllegalArgumentException("审计字段必填");
                    }
                    result.put(field.getKey(), text(MAPPER.valueToTree(field.getValue())));
                }
            }
            return MAPPER.writeValueAsString(result);
        } catch (Exception exception) {
            throw failure(Long.toString(auditId), column, WRITE_FAILURE, exception);
        }
    }

    /**
     * 读取已有审计差异，仅披露白名单字段，数组和对象转换为 JSON 文本。
     * @param auditId 审计 ID
     * @param column 安全列名
     * @param json 数据库存储的审计对象
     * @return 安全差异；null 值保持 null
     */
    public static Map<AuditField, String> readFields(String auditId, String column, String json) {
        JsonNode root = object(auditId, column, json);
        Map<AuditField, String> result = new LinkedHashMap<>();
        root.fields().forEachRemaining(field -> {
            AuditField key;
            try {
                key = AuditField.getEnum(field.getKey());
            } catch (IllegalArgumentException ignored) {
                return;
            }
            result.put(key, text(field.getValue()));
        });
        return result;
    }

    /**
     * 读取字符串版本关联，内部格式损坏返回服务端错误而非 InvalidArgument。
     * @param auditId 审计 ID
     * @param json 版本关联对象
     * @return 版本映射
     */
    public static Map<String, String> readRevisions(String auditId, String json) {
        JsonNode root = object(auditId, REVISIONS_COLUMN, json);
        Map<String, String> result = new LinkedHashMap<>();
        root.fields().forEachRemaining(field -> {
            if (field.getKey().isBlank() || !field.getValue().isTextual() || field.getValue().asText().isBlank()) {
                throw failure(auditId, REVISIONS_COLUMN, READ_FAILURE,
                        new IllegalArgumentException("版本关联格式不合法"));
            }
            result.put(field.getKey(), field.getValue().asText());
        });
        return result;
    }

    private static JsonNode object(String auditId, String column, String json) {
        try {
            JsonNode value = json == null || json.isBlank() ? null : MAPPER.readTree(json);
            if (value == null || !value.isObject()) {
                throw new IllegalArgumentException("审计列必须为对象");
            }
            return value;
        } catch (Exception exception) {
            throw failure(auditId, column, READ_FAILURE, exception);
        }
    }

    private static String text(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isContainerNode() ? value.toString() : value.asText();
    }

    private static BizException failure(String auditId, String column, String message, Exception exception) {
        log.error("[IamAuditValues] auditId={} column={} traceId={} failure={}",
                auditId, column, MDC.get(TRACE_ID), exception.getClass().getSimpleName());
        return new BizException(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }
}
