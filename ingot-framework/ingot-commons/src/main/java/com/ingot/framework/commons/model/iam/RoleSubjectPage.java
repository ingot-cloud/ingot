package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>角色有效接收主体分页；计数只包含当前可披露关系。</p>
 * @param items 当前页去重主体
 * @param total 相同筛选的可见总数
 * @param page 从 1 开始的页码
 * @param pageSize 页大小
 * @param inheritedSourcesRestricted 组继承是否受到组读取资格或范围限制
 * @author jy
 * @since 1.0.0
 */
public record RoleSubjectPage(@NotNull List<@Valid RoleSubjectSummary> items,
        @JsonSerialize(using = IamCountSerializer.class) long total, int page, int pageSize,
        boolean inheritedSourcesRestricted) { }
