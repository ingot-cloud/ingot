package com.ingot.framework.security.credential.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密码校验结果
 *
 * @author jymot
 * @since 2026-01-21
 */
@Data
public class PasswordCheckResult {

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
    public static PasswordCheckResult fail(String reason) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = false;
        result.failureReasons.add(reason);
        return result;
    }

    /**
     * 创建一个失败的结果（带异常信息）
     */
    public static PasswordCheckResult fail(String reason, Exception exception) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = false;
        result.failureReasons.add(reason);
        result.addMetadata("exception", exception.getClass().getSimpleName());
        result.addMetadata("exceptionMessage", exception.getMessage());
        return result;
    }

    /**
     * 创建一个失败的结果（多个原因）
     */
    public static PasswordCheckResult fail(List<String> reasons) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = false;
        result.failureReasons.addAll(reasons);
        return result;
    }

    /**
     * 创建一个有警告的通过结果
     */
    public static PasswordCheckResult warning(String warningMessage, String warningCode) {
        PasswordCheckResult result = new PasswordCheckResult();
        result.passed = true; // 通过但有警告
        result.warnings.add(warningMessage);
        result.addMetadata("warningCode", warningCode);
        return result;
    }

    /**
     * 添加失败原因
     */
    public void addFailureReason(String reason) {
        this.passed = false;
        this.failureReasons.add(reason);
    }

    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
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
