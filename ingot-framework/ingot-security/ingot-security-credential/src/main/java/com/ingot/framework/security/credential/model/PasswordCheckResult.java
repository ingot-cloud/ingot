package com.ingot.framework.security.credential.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.error.BizException;
import lombok.Data;

/**
 * 密码校验结果
 *
 * @author jymot
 * @since 2026-01-21
 */
@Data
public class PasswordCheckResult {
    private static final String METADATA_KEY_FAILURE_CODE = "failureCode";
    private static final String METADATA_KEY_WARNING_CODE = "warningCode";

    /**
     * 是否通过校验
     */
    private boolean passed;

    /**
     * 失败原因列表
     */
    private List<String> failureReasons = new ArrayList<>();

    /**
     * 警告信息列表
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * 元数据（扩展信息）
     */
    private Map<String, Object> metadata = new HashMap<>();

    private PasswordCheckResult() {
    }

    /**
     * 创建一个通过的结果
     */
    public static PasswordCheckResult pass() {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = true;
        return result;
    }

    /**
     * 创建一个失败的结果
     */
    public static PasswordCheckResult fail(String reason, CredentialErrorCode errorCode) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = false;
        result.failureReasons.add(reason);
        result.addMetadata(METADATA_KEY_FAILURE_CODE, errorCode);
        return result;
    }

    /**
     * 创建一个失败的结果（多个原因）
     */
    public static PasswordCheckResult fail(List<String> reasons, CredentialErrorCode errorCode) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = false;
        result.failureReasons.addAll(reasons);
        result.addMetadata(METADATA_KEY_FAILURE_CODE, errorCode);
        return result;
    }

    /**
     * 创建一个有警告的通过结果
     */
    public static PasswordCheckResult warning(String warningMessage, CredentialErrorCode warningCode) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = true; // 通过但有警告
        result.warnings.add(warningMessage);
        result.addMetadata(METADATA_KEY_WARNING_CODE, warningCode);
        return result;
    }

    /**
     * 如果失败则抛出异常
     */
    public PasswordCheckResult ifErrorThrow() {
        if (!isPassed()) {
            throw new BizException(getFailureCode().getCode(), getFailureMessage());
        }
        return this;
    }

    /**
     * 添加失败原因
     */
    public PasswordCheckResult addFailureReason(String reason) {
        this.passed = false;
        this.failureReasons.add(reason);
        return this;
    }

    /**
     * 添加警告信息
     */
    public PasswordCheckResult addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }

    /**
     * 添加元数据
     */
    public PasswordCheckResult addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * 获取警告码
     *
     * @return 警告码
     */
    public CredentialErrorCode getWarningCode() {
        return (CredentialErrorCode) metadata.get(METADATA_KEY_WARNING_CODE);
    }

    /**
     * 获取失败码
     *
     * @return 失败码
     */
    public CredentialErrorCode getFailureCode() {
        return (CredentialErrorCode) metadata.get(METADATA_KEY_FAILURE_CODE);
    }

    /**
     * 是否有警告
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * 获取失败原因消息（合并）
     */
    public String getFailureMessage() {
        return String.join("; ", failureReasons);
    }
}
