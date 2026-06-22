package com.ingot.cloud.pms.api.model.dto.application;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>创建应用内 GROUP / ACTION 权限请求。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "创建应用内权限")
public class AppPermissionCreateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父权限 ID")
    private Long pid;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限编码片段或完整编码；GROUP 须以 :* 结尾")
    private String code;

    @Schema(description = "节点类型：GROUP 或 ACTION")
    private PermissionNodeTypeEnum nodeType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态")
    private CommonStatusEnum status;
}
