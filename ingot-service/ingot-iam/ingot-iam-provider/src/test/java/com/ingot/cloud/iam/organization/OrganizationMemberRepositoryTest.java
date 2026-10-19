package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.*;
import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证组织成员候选及部门导航在SQL分页前按租户、对象范围和分支过滤。</p>
 * @author jy
 * @since 1.0.0
 */
class OrganizationMemberRepositoryTest {
    private OrganizationMemberRepository members;
    private DepartmentQueryRepository departments;
    private MemberQueryRepository memberQueries;
    private JdbcTemplate jdbc;
    private TenantLiveMapper live;
    @BeforeEach
    void database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY,enabled BOOLEAN,deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY,tenant_id BIGINT,account_id BIGINT,display_name VARCHAR(64),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT,member_id BIGINT,department_id BIGINT,is_primary BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT,group_id BIGINT,member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY,tenant_id BIGINT,parent_id BIGINT,name VARCHAR(64),sort_order INT,version BIGINT,created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_account VALUES(1,TRUE,NULL),(2,TRUE,NULL),(3,FALSE,NULL),(4,TRUE,NULL)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES(101,10,1,'张一','ACTIVE'),(102,10,2,'张二','SUSPENDED'),(103,10,3,'停用','ACTIVE'),(201,20,4,'跨租户','ACTIVE')");
        jdbc.update("INSERT INTO iam_department(id,tenant_id,parent_id,name,sort_order,version) VALUES(1,10,NULL,'组织',0,0),(11,10,1,'研发',1,0),(12,10,11,'小组',1,0),(21,20,NULL,'别的组织',0,0)");
        jdbc.update("INSERT INTO iam_member_department VALUES(10,101,11,TRUE),(10,101,12,FALSE),(10,102,11,TRUE),(20,201,21,TRUE)");
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES(10,31,101),(10,31,102),(20,31,201)");
        var memberMapper=IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class);
        members=new OrganizationMemberRepository(memberMapper);
        memberQueries=IamMybatisTestAccess.memberQueries(source);
        live=IamMybatisTestAccess.mapper(source,TenantLiveMapper.class);
        departments=new DepartmentQueryRepository(IamMybatisTestAccess.mapper(source,IamDepartmentMapper.class),IamMybatisTestAccess.mapper(source,IamMemberDepartmentMapper.class),memberMapper);
    }
    @Test
    void activeCandidatesAndKnownSelectedHaveSeparateAvailabilityRules() {
        assertEquals(1,members.page(10,ObjectScope.all(),null,null,List.of(),1,20).getTotal());
        assertEquals(1,members.page(10,ObjectScope.all(),null,null,List.of(id(102)),1,20).getTotal());
        assertEquals(0,members.page(10,ObjectScope.all(),null,null,List.of(id(103),id(201)),1,20).getTotal());
    }
    @Test
    void branchAndScopeApplyBeforePaginationWhileSearchIsOrganizationWide() {
        var none=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(102)),List.of())));
        assertEquals(0,members.page(10,none,11L,null,List.of(),1,1).getTotal());
        assertEquals(1,members.page(10,ObjectScope.all(),12L,null,List.of(),1,1).getTotal());
        assertEquals(1,members.page(10,ObjectScope.all(),99L,"张",List.of(),1,1).getTotal());
    }
    @Test
    void boundGroupMembersArePagedWithinTenantAndMemberScope() {
        assertEquals(2,members.page(10,ObjectScope.all(),null,null,List.of(),1,1,31L).getTotal());
        var onlySuspended=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(102)),List.of())));
        var result=members.page(10,onlySuspended,null,null,List.of(),1,20,31L);
        assertEquals(List.of(id(102)),result.getRecords().stream().map(row->row.getId()).toList());
        assertEquals(0,members.page(10,ObjectScope.all(),null,null,List.of(),1,20,32L).getTotal());
    }
    @Test
    void departmentFilterIntersectsDelegatedRecipientsBeforeCountAndPage() {
        jdbc.execute("CREATE TABLE iam_delegation_recipient_department(tenant_id BIGINT,delegation_id BIGINT,department_id BIGINT,include_descendants BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(tenant_id BIGINT,delegation_id BIGINT,tenant_member_id BIGINT,domain VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT,group_id BIGINT,department_id BIGINT,include_descendants BOOLEAN)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES(104,10,4,'接收人','ACTIVE'),(105,10,1,'非接收人','ACTIVE')");
        jdbc.update("INSERT INTO iam_member_department VALUES(10,104,12,TRUE),(10,105,12,TRUE)");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(10,61,101,'TENANT'),(10,61,104,'TENANT'),(20,61,201,'TENANT')");
        var direct=new TenantLiveOptionSql.Query(id(10),id(101),null,com.ingot.framework.commons.model.iam.TenantAssignmentOptionKind.MEMBER,"%",List.of(),1,0,id(12));
        assertEquals(3,live.optionsCount(direct));
        var delegated=new TenantLiveOptionSql.Query(id(10),id(101),id(61),com.ingot.framework.commons.model.iam.TenantAssignmentOptionKind.MEMBER,"%",List.of(),1,0,id(12));
        assertEquals(1,live.optionsCount(delegated));
        assertEquals(List.of(id(104)),live.options(delegated).stream().map(TenantLiveOptionSql.Option::id).toList());
    }
    @Test
    void organizationNameIsOnlyReadFromVisibleRoot() {
        assertEquals("组织",departments.navigationRoot(10,ObjectScope.all()).getName());
        var branchOnly=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(12)),List.of())));
        assertNull(departments.navigationRoot(10,branchOnly));
        assertNull(departments.navigationRoot(10,ObjectScope.none()));
    }
    @Test
    void navigationOnlyReturnsVisibleBranchesAndDoesNotRevealHiddenAncestors() {
        assertEquals(List.of(id(11)),departments.navigation(10,ObjectScope.all(),null,1,20).getRecords().stream().map(row->row.getId()).toList());
        assertEquals(List.of(id(12)),departments.navigation(10,ObjectScope.all(),11L,1,20).getRecords().stream().map(row->row.getId()).toList());
        var partial=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(12)),List.of())));
        assertEquals(List.of(id(12)),departments.navigation(10,partial,null,1,20).getRecords().stream().map(row->row.getId()).toList());
        assertEquals(0,departments.navigation(10,ObjectScope.none(),null,1,20).getTotal());
    }
    @Test
    void pickerRootUsesActualBindingsBeforePagingWithoutChangingLegacyLists() {
        jdbc.update("INSERT INTO iam_tenant_member VALUES(104,10,1,'零任职','ACTIVE'),(105,10,1,'仅根','ACTIVE'),(106,10,1,'根及研发','ACTIVE'),(107,10,1,'仅研发','ACTIVE'),(108,10,1,'隐藏部门','ACTIVE'),(109,10,1,'子部门','ACTIVE'),(110,10,1,'本租户零任职','ACTIVE')");
        jdbc.update("INSERT INTO iam_member_department VALUES(10,105,1,TRUE),(10,106,1,TRUE),(10,106,11,FALSE),(10,107,11,TRUE),(10,108,99,TRUE),(10,109,12,TRUE),(20,110,21,TRUE)");
        var root=MemberPickerBranch.department(1,true);
        var first=members.page(10,ObjectScope.all(),null,null,List.of(),1,1,null,root);
        assertEquals(4,first.getTotal());
        assertEquals(List.of(id(104)),first.getRecords().stream().map(row->row.getId()).toList());
        assertEquals(List.of(id(104),id(105),id(106),id(110)),members.page(10,ObjectScope.all(),null,null,List.of(),1,20,null,root).getRecords().stream().map(row->row.getId()).toList());
        assertEquals(List.of(id(101),id(106),id(107)),members.page(10,ObjectScope.all(),null,null,List.of(),1,20,null,MemberPickerBranch.department(11,false)).getRecords().stream().map(row->row.getId()).toList());
        assertEquals(Set.of(id(104),id(110)),members.unassigned(10,List.of(id(104),id(105),id(106),id(108),id(110),id(201))));
        var compound=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(104)),List.of(Set.of(id(11))))));
        assertTrue(members.unassigned(10,List.of(id(104)),compound).isEmpty());
        var onlyHidden=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(108)),List.of())));
        assertEquals(0,members.page(10,onlyHidden,null,null,List.of(),1,20,null,root).getTotal());
        assertEquals(0,members.page(10,ObjectScope.all(),null,null,List.of(),1,20,null,MemberPickerBranch.none()).getTotal());
        assertEquals(8,members.page(10,ObjectScope.all(),null,null,List.of(),1,20).getTotal());
    }
    @Test
    void delegatedPickerRootCannotBroadenRecipientsOrIncludeTheActor() {
        jdbc.execute("CREATE TABLE iam_delegation_recipient_department(tenant_id BIGINT,delegation_id BIGINT,department_id BIGINT,include_descendants BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(tenant_id BIGINT,delegation_id BIGINT,tenant_member_id BIGINT,domain VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT,group_id BIGINT,department_id BIGINT,include_descendants BOOLEAN)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES(104,10,1,'可接收零任职','ACTIVE'),(105,10,1,'不可接收零任职','ACTIVE'),(106,10,1,'可接收根及研发','ACTIVE')");
        jdbc.update("INSERT INTO iam_member_department VALUES(10,106,1,TRUE),(10,106,11,FALSE)");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(10,61,101,'TENANT'),(10,61,104,'TENANT'),(10,61,106,'TENANT')");
        var root=MemberPickerBranch.department(1,true);
        var delegated=new TenantLiveOptionSql.Query(id(10),id(104),id(61),com.ingot.framework.commons.model.iam.TenantAssignmentOptionKind.MEMBER,"%",List.of(),1,0,null,root);
        assertEquals(1,live.optionsCount(delegated));
        assertEquals(List.of(id(106)),live.options(delegated).stream().map(TenantLiveOptionSql.Option::id).toList());
        var search=new TenantLiveOptionSql.Query(id(10),id(104),id(61),com.ingot.framework.commons.model.iam.TenantAssignmentOptionKind.MEMBER,"%",List.of(),20,0,null,MemberPickerBranch.all());
        assertEquals(List.of(id(101),id(106)),live.options(search).stream().map(TenantLiveOptionSql.Option::id).toList());
    }
    @Test
    void boundMetadataRetainsRemovedMembersButStillEnforcesCompleteSqlScope() {
        jdbc.update("UPDATE iam_tenant_member SET status='REMOVED' WHERE id=101");
        var ids=List.of(id(101),id(102),id(201));
        assertEquals(Set.of(id(102)),memberQueries.visibleTenantIds(10,ids,ObjectScope.all()));
        var byDepartment=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(),List.of(Set.of(id(11))))));
        assertEquals(Set.of(id(101),id(102)),memberQueries.visibleTenantIds(10,ids,byDepartment,true));
        var intersection=ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(101)),List.of(Set.of(id(11)),Set.of(id(99))))));
        assertTrue(memberQueries.visibleTenantIds(10,ids,intersection,true).isEmpty());
    }
    private static BigInteger id(long value) { return BigInteger.valueOf(value); }
}
