package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回租户管理部门树节点，保留详情信封并嵌套子节点。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param record 部门记录，租户树填充 {@code memberCount}
 * @param fieldAccess 字段访问结果
 * @param capabilities 按操作代码索引的对象能力
 * @param version 求值版本字符串
 * @param children 子部门，叶节点为空数组
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回租户管理部门树节点，保留详情信封并嵌套子节点")
public record DepartmentTreeNode(
        @NotNull @Valid @Schema(description = "部门记录", requiredMode = Schema.RequiredMode.REQUIRED)
        DepartmentRecord record,
        @NotNull @Schema(description = "字段访问结果", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid FieldAccess> fieldAccess,
        @NotNull @Schema(description = "按操作代码索引的对象能力", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid ObjectCapability> capabilities,
        @NotBlank @Schema(description = "求值版本字符串", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotNull @Schema(description = "子部门，叶节点为空数组", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid DepartmentTreeNode> children) {

    /**
     * 复制集合快照，避免外部修改改变已经计算的树。
     */
    public DepartmentTreeNode {
        fieldAccess = fieldAccess == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(fieldAccess));
        capabilities = capabilities == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(capabilities));
        children = children == null ? null : Collections.unmodifiableList(new ArrayList<>(children));
    }

    /**
     * 用详情信封构造树节点。
     *
     * @param detail 部门详情
     * @param children 子节点
     * @return 树节点
     */
    public static DepartmentTreeNode of(ResourceDetail<DepartmentRecord> detail, List<DepartmentTreeNode> children) {
        return new DepartmentTreeNode(detail.record(), detail.fieldAccess(), detail.capabilities(), detail.version(),
                children);
    }
}
