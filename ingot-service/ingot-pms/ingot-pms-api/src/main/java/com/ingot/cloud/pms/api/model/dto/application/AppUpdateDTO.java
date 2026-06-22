package com.ingot.cloud.pms.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>更新应用基本信息请求，仅覆盖显式传入的非空字段，不含应用编码。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "更新应用基本信息")
public class AppUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "应用图标")
    private String icon;

    @Schema(description = "应用介绍")
    private String intro;

    @Schema(description = "排序")
    private Integer sort;
}
