package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * <p>声明必填路径参数及其配置说明，参数值由业务跳转提供。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 参数名，在同一路由内唯一
 * @param remark 配置说明，可空
 */
@Schema(description = "必填路径参数声明")
public record MenuRouteParam(
        @NotBlank @Pattern(regexp = NAME_PATTERN) @Schema(description = "参数名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "参数备注，可空") String remark) {
    /** 参数名允许字母、数字、下划线，首字符不允许数字。 */
    public static final String NAME_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";
}
