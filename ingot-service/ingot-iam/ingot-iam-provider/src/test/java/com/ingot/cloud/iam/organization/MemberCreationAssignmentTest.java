package com.ingot.cloud.iam.organization;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.group.GroupService;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.GroupRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证成员及附带角色分配处于同一数据库事务。</p>
 * @author jy
 * @since 1.0.0
 */
class MemberCreationAssignmentTest {
    @Test
    void failedRoleGrantRollsBackNewPlatformMember() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE member(id BIGINT PRIMARY KEY,account_id BIGINT)");
        var access = mock(IamAccess.class);
        var actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"),
                "0", "0", null);
        when(access.require(eq(AuthorizationDomain.PLATFORM), any())).thenReturn(actor);
        when(access.nextId()).thenReturn(1002L);
        var members = mock(MemberQueryRepository.class);
        when(members.activeAccount(2L)).thenReturn(true);
        doAnswer(invocation -> {
            jdbc.update("INSERT INTO member(id,account_id) VALUES (?,?)", (Object) invocation.getArgument(0),
                    invocation.getArgument(1));
            return null;
        }).when(members).insertPlatform(anyLong(), anyLong(), anyString(), isNull());
        var assignments = mock(AssignmentService.class);
        doThrow(new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE))
                .when(assignments).grantMemberRoleAssignments(anyString(), anyList());
        var service = new MemberQueryService(access, mock(ResourceAccess.class), mock(ObjectCapabilities.class),
                mock(FieldAccessEvaluator.class), mock(IamAuditWriter.class), members,
                mock(GroupRepository.class), assignments, mock(GroupService.class),
                new DataSourceTransactionManager(source));
        var role = new MemberRoleAssignmentDraft("7", new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "8"),
                Map.of(), null, null);
        assertThrows(BizException.class, () -> service.create(AuthorizationDomain.PLATFORM,
                new MemberCreateInput("2", "新人", null, List.of(), List.of(), List.of(), List.of(role))));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM member", Integer.class));
    }
}
