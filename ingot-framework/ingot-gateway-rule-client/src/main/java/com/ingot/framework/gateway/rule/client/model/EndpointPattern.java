package com.ingot.framework.gateway.rule.client.model;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API 路径模式：网关对外 path + HTTP method 二元组。
 *
 * <p>{@code path} 支持 Ant 风格（如 {@code /pms/**}、{@code /auth/*}）。
 * {@code method} 为 {@link #ANY_METHOD}、大写 HTTP 动词或 null 表示不限方法。</p>
 *
 * <p><b>注意</b>：Sentinel Gateway 编译阶段暂不使用 method 过滤，字段保留供
 * {@link com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService} 等使用。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
public class EndpointPattern implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 不限 HTTP 方法的占位符。 */
    public static final String ANY_METHOD = "ANY";

    /** 网关路径 pattern；以 {@code /**} 结尾时走 Ant 前缀匹配。 */
    private String path;

    /** HTTP 方法（大写）或 {@link #ANY_METHOD}。 */
    private String method;

    @JsonCreator
    public EndpointPattern(@JsonProperty("path") String path,
                           @JsonProperty("method") String method) {
        this.path = path;
        this.method = method == null ? ANY_METHOD : method.toUpperCase();
    }

    public static EndpointPattern of(String path, String method) {
        return new EndpointPattern(path, method);
    }

    /** 判断请求 method 是否匹配本 pattern 的 method 约束。 */
    public boolean methodMatches(String requestMethod) {
        return method == null || ANY_METHOD.equalsIgnoreCase(method)
                || method.equalsIgnoreCase(requestMethod);
    }
}
