package com.ingot.cloud.iam.api.model.vo.application;

import java.io.Serial;

import com.ingot.cloud.iam.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.iam.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>应用权限树节点，包含编码、节点类型与可选资源绑定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "应用权限树节点")
public class AppPermissionTreeNodeVO extends TreeNode<Long, AppPermissionTreeNodeVO> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限编码")
    private String code;

    @Schema(description = "节点类型，GROUP 或 ACTION")
    private PermissionNodeTypeEnum nodeType;

    @Schema(description = "组织类型")
    private OrgTypeEnum orgType;

    @Schema(description = "状态")
    private CommonStatusEnum status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关联资源 ID")
    private Long resourceId;
}
