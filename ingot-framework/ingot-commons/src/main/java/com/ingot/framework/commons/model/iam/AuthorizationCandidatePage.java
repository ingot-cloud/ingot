package com.ingot.framework.commons.model.iam;

/**
 * <p>经过当前身份与授权依据筛选的分页候选；未知资源显式标识不支持。</p>
 * @param items 当前页最小候选
 * @param total 同一可见集合的总数
 * @param page 从 1 开始的页码
 * @param pageSize 每页上限
 * @param supported 该资源是否已接入对象查询
 * @param unavailableMessage 不支持配置时的说明
 * @author jy
 * @since 1.0.0
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AuthorizationCandidatePage(@jakarta.validation.constraints.NotNull @jakarta.validation.Valid java.util.List<AuthorizationOption> items, @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = IamCountSerializer.class) @jakarta.validation.constraints.PositiveOrZero long total, @jakarta.validation.constraints.Positive int page, @jakarta.validation.constraints.Positive int pageSize, boolean supported, String unavailableMessage) {
}
