package com.ingot.cloud.pms.api.model.dto.dept;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : DeptWithManagerDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/6.</p>
 * <p>Time         : 18:19.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptWithManagerDTO extends SysDept {
    /**
     * 主管ID
     */
    private List<Long> managerUserIds;
}
