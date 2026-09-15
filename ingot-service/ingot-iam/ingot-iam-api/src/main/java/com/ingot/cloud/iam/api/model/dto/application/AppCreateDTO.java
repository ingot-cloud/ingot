package com.ingot.cloud.iam.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.cloud.iam.api.model.enums.AppDefaultAccessModeEnum;
import com.ingot.cloud.iam.api.model.enums.OrgTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>创建应用请求，根权限自动生成 {@code code:**} 子树通配。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "创建应用")
public class AppCreateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "应用编码，权限命名空间", example = "contacts")
    private String code;

    @Schema(description = "应用名称", example = "通讯录")
    private String name;

    @Schema(description = "应用类型（组织维度）")
    private OrgTypeEnum appType;

    @Schema(description = "应用图标")
    private String icon;

    @Schema(description = "应用介绍")
    private String intro;

    @Schema(description = "排序", example = "100")
    private Integer sort;

    @Schema(description = "无租户覆盖时的默认访问策略，缺省 OPEN")
    private AppDefaultAccessModeEnum defaultAccessMode;
}
