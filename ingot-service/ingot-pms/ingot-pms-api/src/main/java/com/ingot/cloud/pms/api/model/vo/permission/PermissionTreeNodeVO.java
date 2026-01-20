package com.ingot.cloud.pms.api.model.vo.permission;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : AuthorityTreeNodeVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/8/27.</p>
 * <p>Time         : 5:19 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionTreeNodeVO extends TreeNode<Long, PermissionTreeNodeVO> implements PermissionType {

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限编码
     */
    private String code;

    /**
     * 权限类型
     */
    private PermissionTypeEnum type;

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
