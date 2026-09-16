package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回经过数据范围及字段策略过滤后的分页结果。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param <T> 可见的结果类型
 * @param items 当前页可见记录
 * @param total 相同过滤条件下的可见记录总数，JSON 数字
 * @param page 从 1 开始的页码
 * @param pageSize 每页记录上限
 */
public record PageResponse<T>(@NotNull List<@NotNull @Valid T> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) long total,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Positive int page, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Positive int pageSize) {
    /**
     * 固定当前页集合快照；总数必须由应用相同权限过滤的查询计算。
     */
    public PageResponse {
        items = items == null ? null : Collections.unmodifiableList(new ArrayList<>(items));
    }
}
