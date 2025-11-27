package com.ingot.cloud.pms.api.model.vo.dept;

import java.util.List;

import com.ingot.cloud.pms.api.model.vo.user.SimpleUserVO;
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
public class DeptTreeNodeWithManagerVO extends DeptTreeNodeVO {

    /**
     * 部门主管
     */
    private List<SimpleUserVO> managerUsers;

    /**
     * 部门人员数量
     */
    private Long memberCount;
}
