package com.ingot.cloud.pms.api.model.vo.dept;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.vo.user.SimpleUserVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : DeptWithManagerVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/6.</p>
 * <p>Time         : 17:24.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptWithManagerVO extends SysDept {
    /**
     * 部门主管
     */
    private List<SimpleUserVO> managerUsers;
}
