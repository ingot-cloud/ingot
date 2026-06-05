package com.ingot.framework.gateway.rule.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * API 路径模式：包含 path + method 二元组。
 *
 * <p>{@code path} 支持 Ant 风格（{@code /a/*}、{@code /a/**}），
 * {@code method} 大写、{@code "ANY"} 或 {@code null} 表示不限方法。</p>
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

    public static final String ANY_METHOD = "ANY";

    private String path;

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

    public boolean methodMatches(String requestMethod) {
        return method == null || ANY_METHOD.equalsIgnoreCase(method)
                || method.equalsIgnoreCase(requestMethod);
    }
}
