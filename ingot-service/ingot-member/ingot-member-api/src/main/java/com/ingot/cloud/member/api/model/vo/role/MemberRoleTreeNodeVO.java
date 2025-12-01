package com.ingot.cloud.member.api.model.vo.role;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : MemberRoleTreeNodeVO.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "会员角色树节点VO")
public class MemberRoleTreeNodeVO extends TreeNode<Long> {

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String name;

    /**
     * 角色编码
     */
    @Schema(description = "角色编码")
    private String code;

    /**
     * 是否为内置角色
     */
    @Schema(description = "是否内置")
    private Boolean builtIn;

    /**
     * 状态, 0:正常，9:禁用
     */
    @Schema(description = "状态")
    private CommonStatusEnum status;
}

