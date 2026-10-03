package com.ingot.framework.commons.model.iam;

/**
 * <p>经过当前身份与授权依据筛选的分页候选；未知资源显式标识不支持。</p>
 * @param items 当前页最小候选
 * @param total 同一可见集合的总数
 * @param page 从 1 开始的页码
 * @param pageSize 每页上限
 * @param supported 该资源是否已接入对象查询
 * @param unavailableMessage 不支持配置时的说明
 * @param contextLabel 当前候选对应的资源展示名称；其他候选可空
 * @param hierarchical 是否应按父子树展示
 * @author jy
 * @since 1.0.0
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AuthorizationCandidatePage(@jakarta.validation.constraints.NotNull @jakarta.validation.Valid java.util.List<AuthorizationOption> items, @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = IamCountSerializer.class) @jakarta.validation.constraints.PositiveOrZero long total, @jakarta.validation.constraints.Positive int page, @jakarta.validation.constraints.Positive int pageSize, boolean supported, String unavailableMessage, String contextLabel, boolean hierarchical) {
    /**
     * 保持包含上下文名称的普通候选调用契约。
     */
    public AuthorizationCandidatePage(java.util.List<AuthorizationOption> items, long total, int page,
            int pageSize, boolean supported, String unavailableMessage, String contextLabel) {
        this(items, total, page, pageSize, supported, unavailableMessage, contextLabel, false);
    }
    /**
     * 保持现有候选调用方的构造契约。
     * @param items 当前页候选
     * @param total 总数
     * @param page 页码
     * @param pageSize 页大小
     * @param supported 是否支持
     * @param unavailableMessage 不支持说明
     */
    public AuthorizationCandidatePage(java.util.List<AuthorizationOption> items, long total, int page,
            int pageSize, boolean supported, String unavailableMessage) {
        this(items, total, page, pageSize, supported, unavailableMessage, null, false);
    }
}
