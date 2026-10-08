package com.ingot.cloud.bff.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>BFF 登录事务的公开视图，内部秒时间戳转换为接口 UTC 时间点。</p>
 *
 * @param transactionId 事务 ID
 * @param stage 既有登录阶段协议值
 * @param expiresAt UTC 截止时间点
 * @param allows 选择组织阶段的既有候选摘要
 * @param completionUrl 就绪阶段的完成地址
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "BFF 登录事务视图")
public record BffTransactionView(
        @Schema(description = "登录事务 ID") String transactionId,
        @Schema(description = "既有登录阶段协议值") String stage,
        @Schema(description = "事务截止时间，ISO-8601 UTC", type = "string", format = "date-time") Instant expiresAt,
        @Schema(description = "可选组织摘要") List<Map<String, String>> allows,
        @Schema(description = "完成登录地址") String completionUrl) {
}
