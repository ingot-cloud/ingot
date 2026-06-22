package com.ingot.cloud.pms.api.model.vo.application;

import java.io.Serial;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>应用权限树节点，托管权限以 {@code managed}/{@code readOnly} 标记为只读。</p>
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

    @Schema(description = "权限类型")
    private PermissionTypeEnum type;

    @Schema(description = "节点类型")
    private PermissionNodeTypeEnum nodeType;

    @Schema(description = "组织类型")
    private OrgTypeEnum orgType;

    @Schema(description = "状态")
    private CommonStatusEnum status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否系统托管（菜单导航权限）")
    private Boolean managed;

    @Schema(description = "是否只读（托管权限不可通过权限接口修改）")
    private Boolean readOnly;
}
