package com.ingot.cloud.iam.application.organization;

import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentLeaderRepository;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.infrastructure.persistence.MemberLifecycleRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentLeaderMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;

import com.ingot.cloud.iam.application.organization.model.DepartmentDraft;
import com.ingot.cloud.iam.application.organization.model.DepartmentEditInput;
import com.ingot.cloud.iam.application.organization.model.DepartmentLeaderChanges;

import java.util.*;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * <p>通过真实事务和Mapper验证统一保存的一次版本、独立授权与失败回滚。</p>
 * @author jy
 * @since 1.0.0
 */
class DepartmentEditServiceTest {
    private JdbcTemplate jdbc;
    private DepartmentEditService editor;
    private IamAccess access;
    private AuthorizationAuditWriter audits;
    private ResourceScopeAccessService scopes;
    private final ActiveIdentity actor = new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101"), "0", "0", "0");

    @BeforeEach
    void database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY,enabled BOOLEAN,deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY,tenant_id BIGINT,account_id BIGINT,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY,tenant_id BIGINT,parent_id BIGINT,name VARCHAR(64),sort_order INT,version BIGINT,created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_department_leader(tenant_id BIGINT,department_id BIGINT,member_id BIGINT,is_primary BOOLEAN,primary_department_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_primary THEN department_id ELSE NULL END),PRIMARY KEY(tenant_id,department_id,member_id),UNIQUE(tenant_id,primary_department_id))");
        jdbc.update("INSERT INTO iam_account VALUES(1,TRUE,NULL),(2,TRUE,NULL)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES(101,10,1,'ACTIVE'),(102,10,2,'ACTIVE')");
        jdbc.update("INSERT INTO iam_department(id,tenant_id,parent_id,name,sort_order,version) VALUES(1,10,NULL,'组织',0,0),(11,10,1,'研发',1,0)");
        jdbc.update("INSERT INTO iam_department_leader(tenant_id,department_id,member_id,is_primary) VALUES(10,11,101,TRUE)");
        var departments = new DepartmentQueryRepository(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class), mock(IamMemberDepartmentMapper.class), IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class));
        var relations = new DepartmentLeaderRepository(IamMybatisTestAccess.mapper(source,IamDepartmentLeaderMapper.class),IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class));
        access = mock(IamAccess.class); scopes = mock(ResourceScopeAccessService.class); audits = mock(AuthorizationAuditWriter.class);
        when(access.require(eq(AuthorizationDomain.TENANT), any())).thenReturn(actor);
        when(scopes.memberRead(actor.context(),IamAction.TENANT_MEMBER_READ)).thenReturn(ObjectScope.all());
        when(scopes.departmentRead(actor.context(),IamAction.TENANT_DEPARTMENT_READ)).thenReturn(ObjectScope.all());
        var lifecycle = mock(MemberLifecycleRepository.class); when(lifecycle.lockOwner(10)).thenReturn("101");
        var leaderService = new DepartmentLeaderService(access,scopes,departments,relations,lifecycle,mock(FieldAccessEvaluator.class),audits,mock(OrganizationMemberDepartments.class));
        var caps = mock(ObjectCapabilities.class); var snapshot = mock(ObjectCapabilities.Snapshot.class);
        when(caps.snapshot(actor.context())).thenReturn(snapshot);
        when(caps.departments(eq(snapshot),anyList())).thenReturn(Map.of("11",Map.of()));
        var manager = new DataSourceTransactionManager(source);
        var profiles = new DepartmentService(access,scopes,caps,audits,departments,manager,leaderService,relations);
        var service = new DepartmentEditService(access,scopes,lifecycle,departments,profiles,leaderService);
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(manager,new AnnotationTransactionAttributeSource()));
        editor = (DepartmentEditService)proxy.getProxy();
    }

    @Test
    void bothPartsAdvanceOneVersionAndKeepUnchangedPrimary() {
        var result = editor.edit("11",new DepartmentEditInput("0",new DepartmentDraft("1","产品",2),changes()));
        assertEquals("1",result.version()); assertEquals("产品",result.record().name());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM iam_department_leader",Integer.class));
        assertEquals(101L,jdbc.queryForObject("SELECT member_id FROM iam_department_leader WHERE is_primary=TRUE",Long.class));
        assertEquals(1,jdbc.queryForObject("SELECT version FROM iam_department WHERE id=11",Integer.class));
    }

    @Test
    void basicOnlyDoesNotRequireLeaderOrMemberRights() {
        when(access.require(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_LEADERS)).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        when(access.require(AuthorizationDomain.TENANT,IamAction.TENANT_MEMBER_READ)).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        assertEquals("1",editor.edit("11",new DepartmentEditInput("0",new DepartmentDraft("1","产品",2),null)).version());
        verify(access,never()).require(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_LEADERS);
        verify(access,never()).require(AuthorizationDomain.TENANT,IamAction.TENANT_MEMBER_READ);
    }

    @Test
    void leaderOnlyDoesNotRequireProfileWriteEvenWhenIdenticalProfileIsSubmitted() {
        when(access.require(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_UPDATE)).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        assertEquals("1",editor.edit("11",new DepartmentEditInput("0",new DepartmentDraft("1","研发",1),changes())).version());
        verify(access,never()).require(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_UPDATE);
    }

    @Test
    void profileDenialRollsBackEarlierLeaderMutation() {
        when(access.require(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_UPDATE)).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        assertThrows(BizException.class,()->editor.edit("11",new DepartmentEditInput("0",new DepartmentDraft("1","产品",2),changes())));
        assertOriginal();
    }

    @Test
    void auditFailureRollsBackProfileRelationsAndVersion() {
        doNothing().doThrow(new IllegalStateException("审计不可用")).when(audits).write(any(),anyLong(),anyString(),anyString(),any(),anyMap(),anyMap(),anyMap());
        assertThrows(IllegalStateException.class,()->editor.edit("11",new DepartmentEditInput("0",new DepartmentDraft("1","产品",2),changes())));
        assertOriginal();
    }

    @Test
    void hiddenDepartmentIsDeniedBeforeDisclosingVersionConflict() {
        var hidden = new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        doThrow(hidden).when(scopes).requireVisibleDepartment(actor.context(),IamAction.TENANT_DEPARTMENT_READ,11);
        assertSame(hidden,assertThrows(BizException.class,()->editor.edit("11",new DepartmentEditInput("9",new DepartmentDraft("1","产品",2),changes()))));
        verifyNoInteractions(audits);
        assertOriginal();
    }

    @Test
    void staleVersionAndUnavailableNewLeaderCannotPartiallySave() {
        assertThrows(BizException.class,()->editor.edit("11",new DepartmentEditInput("9",new DepartmentDraft("1","产品",2),changes())));
        jdbc.update("UPDATE iam_account SET enabled=FALSE WHERE id=2");
        assertThrows(BizException.class,()->editor.edit("11",new DepartmentEditInput("0",new DepartmentDraft("1","产品",2),changes())));
        assertOriginal();
    }
    private void assertOriginal() {
        assertEquals("研发",jdbc.queryForObject("SELECT name FROM iam_department WHERE id=11",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT version FROM iam_department WHERE id=11",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iam_department_leader",Integer.class));
    }
    private static DepartmentLeaderChanges changes() { return new DepartmentLeaderChanges(List.of("102"),List.of(),null); }
}
