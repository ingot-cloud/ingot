package com.ingot.cloud.pms.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>应用启用/禁用状态变更请求。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "应用状态变更")
public class AppStatusPatchDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标状态")
    private CommonStatusEnum status;
}
