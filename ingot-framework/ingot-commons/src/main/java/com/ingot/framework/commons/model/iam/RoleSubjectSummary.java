package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;

/**
 * <p>描述角色工作区中去重后的有效成员或用户组及其可披露来源摘要。</p>
 * @param id 当前主体 ID
 * @param name 最小显示名称
 * @param revisionNumbers 有效固定版本号
 * @param sourceTypes MEMBER 表示直接分配，GROUP 表示组继承或组分配
 * @param sourceCount 当前可见有效分配数量
 * @author jy
 * @since 1.0.0
 */
public record RoleSubjectSummary(@NotNull String id, @NotNull String name,
        @NotNull @JsonSerialize(contentUsing = IamCountSerializer.class) List<Long> revisionNumbers, @NotNull List<SubjectType> sourceTypes, @JsonSerialize(using = IamCountSerializer.class) long sourceCount) { }
