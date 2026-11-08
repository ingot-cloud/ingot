package com.ingot.cloud.iam.application.organization;

import java.util.List;

import com.ingot.cloud.iam.application.asset.IamImageBinding;
import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService;
import com.ingot.cloud.iam.application.group.GroupService;
import com.ingot.cloud.iam.infrastructure.persistence.MemberRepository;
import com.ingot.framework.authorization.field.FieldProjectionEngine;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * <p>验证资料命令拆分后仍由事务管理器装配，不要求额外声明事务模板 Bean。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberProfileCommandWiringTest {
    @Test
    void productionConstructorUsesTheTransactionManagerWithoutTemplateBean() {
        try (var context = new AnnotationConfigApplicationContext()) {
            for (var type : List.of(MemberQueryService.class, RoleFieldPermissionService.class,
                    FieldProjectionEngine.class, FieldWriteExecutor.class, IamAccess.class,
                    ResourceScopeAccessService.class, AuthorizationAuditWriter.class, MemberRepository.class,
                    PlatformAssignmentFacade.class, GroupService.class, PlatformTransactionManager.class,
                    IamImageBinding.class)) {
                register(context, type);
            }
            context.register(MemberProfileCommandService.class);
            context.refresh();
            assertNotNull(context.getBean(MemberProfileCommandService.class));
            assertTrue(context.getBeansOfType(TransactionTemplate.class).isEmpty());
        }
    }

    private static <T> void register(AnnotationConfigApplicationContext context, Class<T> type) {
        context.registerBean(type, () -> mock(type));
    }
}
