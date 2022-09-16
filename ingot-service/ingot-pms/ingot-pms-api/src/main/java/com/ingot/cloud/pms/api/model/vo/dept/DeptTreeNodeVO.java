package com.ingot.cloud.pms.api.model.vo.dept;

import com.ingot.cloud.pms.api.model.base.TreeNode;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : DeptTreeNodeVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 10:14 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptTreeNodeVO extends TreeNode {

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门角色范围, 0:当前部门，1:当前部门和直接子部门
     */
    private DeptRoleScopeEnum scope;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
}
