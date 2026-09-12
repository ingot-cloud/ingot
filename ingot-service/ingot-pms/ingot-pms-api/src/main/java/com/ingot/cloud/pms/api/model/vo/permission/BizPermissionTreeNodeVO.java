package com.ingot.cloud.pms.api.model.vo.permission;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionNodeTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>角色权限树节点，在权限树上标记是否由平台角色绑定及是否预设。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizPermissionTreeNodeVO extends TreeNode<Long, BizPermissionTreeNodeVO> implements PermissionType {

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

    /**
     * 是否为平台角色绑定的权限
     */
    private boolean platformRoleBind;

    /**
     * 是否为预设权限
     */
    private Boolean defaultFlag;
}
