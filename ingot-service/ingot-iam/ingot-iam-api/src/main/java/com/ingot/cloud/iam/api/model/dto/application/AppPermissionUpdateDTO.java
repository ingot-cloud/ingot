package com.ingot.cloud.iam.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>更新应用权限请求，仅覆盖显式传入的非空字段；不可改编码、应用或资源绑定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "更新应用内权限")
public class AppPermissionUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private CommonStatusEnum status;
}
