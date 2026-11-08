package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.asset.IamImageBinding;

import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;
import com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService;
import com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.group.GroupService;
import com.ingot.cloud.iam.infrastructure.persistence.GroupRepository;
import com.ingot.cloud.iam.infrastructure.persistence.MemberRepository;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;


import com.ingot.cloud.iam.application.organization.MemberFacade;
import com.ingot.cloud.iam.application.organization.MemberQueryService;
import com.ingot.cloud.iam.application.organization.MemberProfileCommandService;

/**
 * <p>在测试中装配成员查询与变更服务，不改变生产构造注入契约。</p>
 * @author jy
 * @since 1.0.0
 */
public final class MemberFacadeFixture {
    private MemberFacadeFixture() { }

    /** 用原夹具依赖建立成员用例聚合。 */
    public static MemberFacade create(IamAccess access,
            ResourceScopeAccessService scopes,
            ObjectCapabilities capabilities,
            FieldAccessEvaluator fields,
            AuthorizationAuditWriter audits,
            MemberRepository members,
            GroupRepository groups,
            PlatformAssignmentFacade assignments,
            GroupService groupCommands,
            PlatformTransactionManager transactionManager,
            RoleFieldPermissionService platformFields,
            com.ingot.framework.authorization.field.FieldProjectionEngine projection,
            IamImageBinding images,
            com.ingot.framework.authorization.field.FieldWriteExecutor fieldWrites) {
        var queries = new MemberQueryService(platformFields, access, scopes, capabilities, fields, members, groups, assignments);
        var commands = new MemberProfileCommandService(platformFields, projection, fieldWrites, access, scopes, audits, members, assignments, groupCommands, transactionManager, images, queries);
        return new MemberFacade(queries, commands);
    }
}
