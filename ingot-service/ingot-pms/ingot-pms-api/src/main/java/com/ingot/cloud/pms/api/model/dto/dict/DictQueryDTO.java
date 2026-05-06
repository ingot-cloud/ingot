package com.ingot.cloud.pms.api.model.dto.dict;

import java.io.Serial;
import java.io.Serializable;

import com.ingot.cloud.pms.api.model.enums.DictScopeEnum;
import com.ingot.cloud.pms.api.model.enums.DictTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典查询条件 DTO，前端管理页/RPC 通用。
 *
 * @author jy
 * @since 2026/4/25
 */
@Data
@Schema(description = "字典查询条件")
public class DictQueryDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字典编码（精确匹配）")
    private String code;

    @Schema(description = "名称（前缀匹配）")
    private String keyword;

    @Schema(description = "节点类型")
    private DictTypeEnum type;

    @Schema(description = "作用域，缺省按平台级返回")
    private DictScopeEnum scopeType;

    @Schema(description = "租户ID（scopeType=TENANT 时必填）")
    private Long tenantId;

    @Schema(description = "应用ID（scopeType=APP 时必填）")
    private Long appId;

    @Schema(description = "组织类型")
    private OrgTypeEnum orgType;

    @Schema(description = "状态，缺省返回所有可见状态（管理端可显式查询禁用项）")
    private CommonStatusEnum status;
}
