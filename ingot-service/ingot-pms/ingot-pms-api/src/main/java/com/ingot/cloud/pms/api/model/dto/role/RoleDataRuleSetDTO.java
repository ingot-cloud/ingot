package com.ingot.cloud.pms.api.model.dto.role;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>角色数据规则整体替换请求，只覆盖当前操作者可管理的来源层。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Schema(description = "角色数据规则整体设置")
public class RoleDataRuleSetDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "该来源层的完整规则集，空列表表示清空该层")
    private List<RoleDataRuleItemDTO> items;
}
