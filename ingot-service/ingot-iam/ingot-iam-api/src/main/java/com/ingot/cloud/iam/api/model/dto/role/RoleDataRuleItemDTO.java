package com.ingot.cloud.iam.api.model.dto.role;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>单条角色数据范围规则，按权限与资源定义范围类型。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "角色数据规则项")
public class RoleDataRuleItemDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "功能权限 ID")
    private Long permissionId;

    @Schema(description = "资源 ID")
    private Long resourceId;

    @Schema(description = "数据范围类型")
    private DataScopeTypeEnum scopeType;

    @Schema(description = "CUSTOM 时的部门 ID 列表，其它类型为空")
    private List<Long> scopes;
}
