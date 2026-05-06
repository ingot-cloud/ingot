package com.ingot.cloud.pms.api.model.vo.dict;

import java.util.Map;

import com.ingot.cloud.pms.api.model.enums.DictScopeEnum;
import com.ingot.cloud.pms.api.model.enums.DictTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典树节点 VO。
 *
 * @author jy
 * @since 2026/4/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "字典树节点")
public class DictTreeNodeVO extends TreeNode<Long, DictTreeNodeVO> {

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "字典项值")
    private String value;

    @Schema(description = "字典项展示文本")
    private String label;

    @Schema(description = "字典节点类型")
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
