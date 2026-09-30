package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * <p>按当前可信身份和单条授权依据筛选的角色树层级分页。</p>
 *
 * @param items 当前层的最小节点
 * @param total 同一可见集合总数
 * @param page 从 1 开始的页码
 * @param pageSize 每页上限
 * @author jy
 * @since 1.0.0
 */
@Schema(description = "授权角色树层级分页")
public record AuthorizationRoleCandidatePage(
        @NotNull List<@Valid AuthorizationRoleNode> items,
        @JsonSerialize(using = IamCountSerializer.class) @PositiveOrZero long total,
        @Positive int page,
        @Positive int pageSize) {
}
