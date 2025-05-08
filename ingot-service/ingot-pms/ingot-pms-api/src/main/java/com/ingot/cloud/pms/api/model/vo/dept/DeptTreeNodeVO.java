package com.ingot.cloud.pms.api.model.vo.dept;

import com.ingot.cloud.pms.api.model.vo.user.SimpleUserVO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.tree.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : DeptTreeNodeVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/25.</p>
 * <p>Time         : 10:14 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptTreeNodeVO extends TreeNode<Long> {

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

    /**
     * 部门主管
     */
    private List<SimpleUserVO> managerUsers;

    /**
     * 部门人员数量
     */
    private Long memberCount;
}
