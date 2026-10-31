package com.ingot.cloud.bff.model.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ingot.framework.commons.model.bff.BffLoginStage;

/**
 * <p>
 * 登录结果的公开判别视图，不含任何会话或授权秘密。
 * </p>
 *
 * @param transactionId 本次不透明事务
 * @param stage 服务端阶段
 * @param expiresAt 未完成认证的 UTC 截止
 * @param allows 选择阶段的候选摘要
 * @param returnTo 固定站内落地路径
 * @param sessionEstablished 实际 Cookie 是否属于本次结果
 * @param recoveryExpiresAt 已完成结果的恢复期限
 * @param retryAfterMs 处理中建议查询间隔
 * @author jy
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BffTransactionView(String transactionId, BffLoginStage stage, Instant expiresAt,
        List<Map<String, String>> allows, String returnTo, Boolean sessionEstablished, Instant recoveryExpiresAt,
        Long retryAfterMs) {
}
