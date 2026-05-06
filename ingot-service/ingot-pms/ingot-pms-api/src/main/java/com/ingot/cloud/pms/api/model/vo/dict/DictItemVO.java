package com.ingot.cloud.pms.api.model.vo.dict;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import com.ingot.cloud.pms.api.model.enums.DictScopeEnum;
import com.ingot.cloud.pms.api.model.enums.DictTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典项 VO，前端管理页与 RPC 共用的稳定模型。
 *
 * @author jy
 * @since 2026/4/25
 */
@Data
@Schema(description = "字典项")
public class DictItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "父ID")
    private Long pid;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "字典项值")
    private String value;

    @Schema(description = "字典项展示文本")
    private String label;

    @Schema(description = "字典节点类型，TYPE 类型 / ITEM 字典项")
    private DictTypeEnum type;

    @Schema(description = "作用域")
    private DictScopeEnum scopeType;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "应用ID")
    private Long appId;

    @Schema(description = "组织类型")
    private OrgTypeEnum orgType;

    @Schema(description = "排序权重")
    private Integer sort;

    @Schema(description = "是否内置字典")
    private Boolean systemFlag;

    @Schema(description = "状态")
    private CommonStatusEnum status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "扩展属性")
    private Map<String, Object> extra;
}
