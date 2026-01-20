package com.ingot.cloud.pms.api.model.vo.dept;

import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : DeptTreeNodeVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/27.</p>
 * <p>Time         : 14:40.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptTreeNodeVO extends TreeNode<Long, DeptTreeNodeVO> {

    /**
     * 部门名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 主部门标识
     */
    private Boolean mainFlag;

    /**
     * 状态, 0:正常，9:禁用
     */
    private CommonStatusEnum status;
}
