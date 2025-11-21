package com.ingot.cloud.pms.api.model.dto.dept;

import com.ingot.cloud.pms.api.model.domain.TenantDept;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : DeptWithMemberCountDTO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/5/8.</p>
 * <p>Time         : 13:39.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptWithMemberCountDTO extends TenantDept {
    /**
     * 部门人员数量
     */
    private Long memberCount;
}
