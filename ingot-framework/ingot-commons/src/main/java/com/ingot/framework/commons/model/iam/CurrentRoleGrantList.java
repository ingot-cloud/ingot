package com.ingot.framework.commons.model.iam;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回角色当前完整权限与目录，不带权限版本。</p>
 * @author jy
 * @since 1.0.0
 * @param items 完整结果或输入集合
 * @param definition 角色完整当前定义
 */
@Schema(description = "返回角色当前完整权限与目录，不带权限版本")
public record CurrentRoleGrantList(
        List<CurrentRoleCatalogAction> items,
        CurrentRoleDefinition definition) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public CurrentRoleGrantList {
        if (items != null) items = List.copyOf(items);
    }
}
