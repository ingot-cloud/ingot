package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>承载当前域内的成员和部门选择，不展开成员或授予对象访问权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param members 成员 ID 集合，平台域使用平台成员；省略表示空集合
 * @param departments 租户部门集合，平台域必须为空；省略表示空集合
 */
@Schema(description = "当前域内的成员与部门选择")
public record Selection(
        @Schema(description = "成员 ID 集合，默认空数组") List<@NotBlank String> members,
        @Schema(description = "部门集合，平台域必须为空")
        List<@NotNull @Valid DepartmentSelection> departments) {

    /**
     * 规范化可选集合并复制内容，保证后续外部修改不改变选择结果。
     */
    public Selection {
        members = members == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(members));
        departments = departments == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(departments));
    }

    /**
     * 检查选择器结构与管理域兼容，成员及部门是否真实属于当前域仍需服务端查询校验。
     *
     * @param domain 已认证的管理域
     * @throws IllegalArgumentException 域缺失或平台选择器携带部门
     */
    public void requireCompatibleDomain(AuthorizationDomain domain) {
        if (domain == null) {
            throw new IllegalArgumentException("选择器的管理域必填");
        }
        if (domain == AuthorizationDomain.PLATFORM && !departments.isEmpty()) {
            throw new IllegalArgumentException("平台选择器不能引用租户部门");
        }
    }
}
