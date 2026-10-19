package com.ingot.cloud.iam.organization;

import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>以同一组织锁、版本及事务保存部门资料和负责人，按实际差量分别授权。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DepartmentEditService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final MemberLifecycleRepository lifecycle;
    private final DepartmentQueryRepository departments;
    private final DepartmentService profiles;
    private final DepartmentLeaderService leaders;

    /** 任意部分失败时包含关系、资料、版本和审计的全部修改均回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public ResourceDetail<DepartmentRecord> edit(String id, DepartmentEditInput input) {
        if (!input.isChangeProvided()) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
        long tenant = IamIds.require(actor.context().tenantId()), departmentId = IamIds.require(id);
        if (lifecycle.lockOwner(tenant) == null) throw new BizException(IamReasonCode.IDENTITY_INVALID);
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_DEPARTMENT_READ);
        var row = departments.lock(tenant, departmentId);
        if (row == null) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        scopes.requireVisibleDepartment(actor.context(), IamAction.TENANT_DEPARTMENT_READ, departmentId);
        IamIds.requireVersion(input.expectedVersion(), row.getVersion().toString());
        boolean changed = input.leaders() != null && leaders.editLocked(actor, row, input.leaders());
        if (input.department() != null) changed = profiles.editProfileLocked(actor, row, input.department()) || changed;
        if (changed) departments.advanceVersion(tenant, departmentId, row.getVersion());
        return profiles.get(id);
    }
}
