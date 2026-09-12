package com.ingot.cloud.pms.api.model.vo.permission;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>全量权限树节点，含节点类型与可选资源绑定。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionTreeNodeVO extends TreeNode<Long, PermissionTreeNodeVO> implements PermissionType {

    /**
     * 所属应用
     */
    private Long appId;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限编码
     */
    private String code;

    /**
     * 节点类型，GROUP 或 ACTION
     */
    private PermissionNodeTypeEnum nodeType;

    /**
     * 关联资源 ID，非数据操作可空
     */
    private Long resourceId;

    /**
     * 组织类型
     */
    private OrgTypeEnum orgType;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 备注
     */
    private String remark;
}
