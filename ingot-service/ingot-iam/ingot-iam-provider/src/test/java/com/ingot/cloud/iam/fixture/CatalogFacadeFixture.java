package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.catalog.CatalogFacade;

import com.ingot.cloud.iam.application.asset.IamImageBinding;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.infrastructure.persistence.CatalogRepository;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.SensitiveConfirmationGuard;
import org.springframework.transaction.PlatformTransactionManager;
import com.ingot.cloud.iam.application.catalog.*;

/**
 * <p>为原有catalog回归构造明确的应用服务图，不增加生产注入入口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class CatalogFacadeFixture {
    private CatalogFacadeFixture() { }

    /** 使用既有测试依赖装配拆分后的用例。 */
    public static CatalogFacade create(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
            CatalogRepository catalog, SensitiveConfirmationGuard confirmations,
            com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata,
            IamImageBinding images, PlatformTransactionManager transactionManager) {
        CatalogOperations operations = new CatalogOperations(access, audits, changes, catalog, confirmations, fieldMetadata, images, transactionManager);
        ActionCatalogService actionCatalogService = new ActionCatalogService(operations);
        ApplicationBundleService applicationBundleService = new ApplicationBundleService(operations);
        ApplicationCatalogService applicationCatalogService = new ApplicationCatalogService(operations);
        MenuCatalogService menuCatalogService = new MenuCatalogService(operations);
        PlanCatalogService planCatalogService = new PlanCatalogService(operations);
        ResourceCatalogService resourceCatalogService = new ResourceCatalogService(operations);
        return new CatalogFacade(actionCatalogService, applicationBundleService, applicationCatalogService, menuCatalogService, planCatalogService, resourceCatalogService);
    }
}
