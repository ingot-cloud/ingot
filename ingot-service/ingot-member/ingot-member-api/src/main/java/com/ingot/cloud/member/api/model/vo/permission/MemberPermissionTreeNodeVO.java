package com.ingot.cloud.member.api.model.vo.permission;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : MemberPermissionTreeNodeVO.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "会员权限树节点VO")
public class MemberPermissionTreeNodeVO extends TreeNode<Long> {
    /**
     * 权限名称
     */
    @Schema(description = "权限名称")
    private String name;

    /**
     * 权限编码
     */
    @Schema(description = "权限编码")
    private String code;

    /**
     * 类型
     */
    @Schema(description = "权限类型")
    private PermissionTypeEnum type;

    /**
     * 状态, 0:正常，9:禁用
     */
    @Schema(description = "状态")
    private CommonStatusEnum status;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
}

