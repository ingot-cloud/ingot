package com.ingot.cloud.iam.application.authorization.evaluation;

import com.ingot.cloud.iam.domain.authorization.ResolvedActionScope;
import com.ingot.cloud.iam.domain.authorization.ScopeClause;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeCompiler;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.infrastructure.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.infrastructure.persistence.MemberRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>以实际成员SQL验证可查看目标的编辑越界403与不可查看对象404。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformMemberUpdateAccessTest {
    private final AuthorizationContext actor = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private static MemberRepository members;
    private ResourceScopeAccessService access;

    @BeforeAll
    static void database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,status VARCHAR(16))");
        jdbc.update("INSERT INTO iam_platform_member VALUES(1001,'ACTIVE'),(1002,'ACTIVE')");
        members = IamMybatisTestAccess.memberQueries(source);
    }

    @BeforeEach
    void setup() {
        access = new ResourceScopeAccessService(evaluator,
                new ObjectScopeCompiler(mock(DepartmentClosure.class), mock(IamMemberDepartmentMapper.class)),
                members, mock(DepartmentQueryRepository.class));
        authorize(new ResolvedActionScope(List.of(ScopeClause.universe())));
    }

    @Test
    void readableTargetOutsideUpdateScopeReturnsScopeDenied() {
        assertDoesNotThrow(() -> access.requirePlatformMemberUpdate(actor, 1001));
        var error = assertThrows(BizException.class, () -> access.requirePlatformMemberUpdate(actor, 1002));
        assertEquals(IamReasonCode.DATA_SCOPE_DENIED.getCode(), error.getCode());
        verify(evaluator, times(2)).evaluateForExecution(actor, true);
    }

    @Test
    void hiddenOrMissingTargetKeepsNotFound() {
        authorize(ResolvedActionScope.empty());
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(),
                assertThrows(BizException.class, () -> access.requirePlatformMemberUpdate(actor, 1002)).getCode());
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(),
                assertThrows(BizException.class, () -> access.requirePlatformMemberUpdate(actor, 9999)).getCode());
    }

    @Test
    void missingUpdateActionDeniesBeforeObjectLookup() {
        doThrow(new BizException(IamReasonCode.ACTION_DENIED)).when(evaluator).require(actor, IamAction.PLATFORM_MEMBER_UPDATE);
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(),
                assertThrows(BizException.class, () -> access.requirePlatformMemberUpdate(actor, 1002)).getCode());
        verify(evaluator, never()).evaluateForExecution(any(), anyBoolean());
    }

    private void authorize(ResolvedActionScope read) {
        var write = new ResolvedActionScope(List.of(new ScopeClause(false, false, false, false, List.of(), false, List.of("1001"))));
        var actions = List.of(IamAction.VALUE_PLATFORM_MEMBER_READ, IamAction.VALUE_PLATFORM_MEMBER_UPDATE);
        when(evaluator.evaluateForExecution(actor, true)).thenReturn(new AuthorizationView(
                actions, actions, Map.of(IamAction.VALUE_PLATFORM_MEMBER_READ, read, IamAction.VALUE_PLATFORM_MEMBER_UPDATE, write),
                "1", Instant.now().plusSeconds(60)));
    }
}
