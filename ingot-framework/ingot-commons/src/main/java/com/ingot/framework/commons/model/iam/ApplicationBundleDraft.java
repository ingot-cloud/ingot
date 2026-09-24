package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>一次提交应用及其资源、操作与菜单，由服务器在同一事务中整单创建。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param application 应用草稿
 * @param resources 资源及内嵌操作，可空
 * @param menus 菜单树草稿，可空
 */
@Schema(description = "一次提交应用及其资源、操作与菜单，由服务器在同一事务中整单创建")
public record ApplicationBundleDraft(
        @NotNull @Valid @Schema(description = "应用草稿", requiredMode = Schema.RequiredMode.REQUIRED)
        ApplicationDraft application,
        @NotNull @Schema(description = "资源及内嵌操作，可空", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ApplicationBundleResource> resources,
        @NotNull @Schema(description = "菜单树草稿，可空", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ApplicationBundleMenu> menus) {

    /**
     * 复制资源和菜单列表。
     */
    public ApplicationBundleDraft {
        if (resources != null) {
            resources = Collections.unmodifiableList(new ArrayList<>(resources));
        }
        if (menus != null) {
            menus = Collections.unmodifiableList(new ArrayList<>(menus));
        }
    }
}
