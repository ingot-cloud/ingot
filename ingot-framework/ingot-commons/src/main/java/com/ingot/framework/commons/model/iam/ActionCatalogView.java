package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>一次返回某应用的资源及操作树，供菜单选择操作，不走分页列表拼装。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param applicationId 应用 ID
 * @param applicationCode 应用编码
 * @param applicationName 应用名称
 * @param resources 资源及其操作
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "一次返回某应用的资源及操作树，供菜单选择操作，不走分页列表拼装")
public record ActionCatalogView(
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotBlank @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationCode,
        @NotBlank @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationName,
        @NotNull @Schema(description = "资源及其操作", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionCatalogResource> resources) {

    /**
     * 复制资源列表，避免外部修改改变已经计算的目录视图。
     */
    public ActionCatalogView {
        if (resources != null) {
            resources = Collections.unmodifiableList(new ArrayList<>(resources));
        }
    }
}
