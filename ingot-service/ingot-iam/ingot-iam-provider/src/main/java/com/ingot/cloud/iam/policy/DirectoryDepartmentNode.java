package com.ingot.cloud.iam.policy;

import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;

/**
 * <p>通讯录部门树节点，{@code navigationOnly} 仅标识必要祖先骨架。</p>
 *
 * @param row 部门行
 * @param navigationOnly 是否没有可见成员、仅作为祖先导航
 * @author jy
 * @since 1.0.0
 */
public record DirectoryDepartmentNode(IamDepartmentEntity row, boolean navigationOnly) {
}
