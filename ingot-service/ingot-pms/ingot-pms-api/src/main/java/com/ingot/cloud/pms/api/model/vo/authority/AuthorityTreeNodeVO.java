package com.ingot.cloud.pms.api.model.vo.authority;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.tree.TreeNode;
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
public class AuthorityTreeNodeVO extends TreeNode<Long> implements AuthorityType {

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
    private OrgTypeEnum type;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;

    /**
     * 备注
     */
    private String remark;
}
