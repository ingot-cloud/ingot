package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.role.PlatformRoleFacade;

import com.ingot.cloud.iam.infrastructure.cache.RoleSynthesisCache;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.catalog.CatalogFacade;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import org.springframework.transaction.PlatformTransactionManager;
import com.ingot.cloud.iam.application.role.*;

/**
 * <p>为原有role回归构造明确的应用服务图，不增加生产注入入口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class PlatformRoleFacadeFixture {
    private PlatformRoleFacadeFixture() { }

    /** 使用既有测试依赖装配拆分后的用例。 */
    public static PlatformRoleFacade create(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
            RoleSynthesisCache synthesis, RoleGrantValidator validator,
            RoleRepository roles, CatalogFacade catalog,
            com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService roleFields,
            com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata,
            PlatformTransactionManager transactionManager) {
        PlatformRoleOperations operations = new PlatformRoleOperations(access, audits, changes, synthesis, validator, roles, catalog, roleFields, fieldMetadata, transactionManager);
        PlatformRoleCommandService platformRoleCommandService = new PlatformRoleCommandService(operations);
        PlatformRolePreviewService platformRolePreviewService = new PlatformRolePreviewService(operations);
        PlatformRoleRevisionService platformRoleRevisionService = new PlatformRoleRevisionService(operations);
        PlatformRoleQueryService platformRoleQueryService = new PlatformRoleQueryService(operations, platformRoleRevisionService);
        return new PlatformRoleFacade(platformRoleCommandService, platformRolePreviewService, platformRoleQueryService, platformRoleRevisionService);
    }
}
