package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;
import com.ingot.cloud.iam.persistence.DepartmentLeaderRepository;
import com.ingot.cloud.iam.persistence.DepartmentLeaderSql;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentLeaderMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>使用真实 MyBatis 和数据库验证负责人分页隔离、交集范围及事务回滚。</p>
 * @author jy
 * @since 1.0.0
 */
class DepartmentLeaderRepositoryTest {
    private DepartmentLeaderRepository repository;
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;

    @BeforeEach
    void database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY,enabled BOOLEAN,deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY,tenant_id BIGINT,account_id BIGINT,display_name VARCHAR(100),avatar VARCHAR(512),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY,tenant_id BIGINT,version BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT,member_id BIGINT,department_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_department_leader(tenant_id BIGINT,department_id BIGINT,member_id BIGINT,is_primary BOOLEAN,primary_department_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_primary THEN department_id ELSE NULL END),PRIMARY KEY(tenant_id,department_id,member_id),UNIQUE(tenant_id,primary_department_id))");
        jdbc.update("INSERT INTO iam_account VALUES(1,TRUE,NULL),(2,TRUE,NULL),(3,FALSE,NULL),(4,TRUE,NULL),(5,TRUE,NULL),(6,TRUE,NULL),(7,TRUE,NULL)");
        jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name,status) VALUES(101,10,1,'研发主管','ACTIVE'),(102,10,2,'暂停负责人','SUSPENDED'),(103,10,3,'停用账号','ACTIVE'),(104,10,4,'跨部兼管','ACTIVE'),(105,10,5,'离职负责人','REMOVED'),(106,10,6,'新候选','ACTIVE'),(201,20,7,'其他租户','ACTIVE')");
        jdbc.update("INSERT INTO iam_department VALUES(11,10,0),(12,10,0),(21,20,0)");
        jdbc.update("INSERT INTO iam_member_department VALUES(10,101,11),(10,101,12),(10,102,11),(10,103,11),(10,104,12),(10,105,11),(10,106,11),(20,201,21)");
        jdbc.update("INSERT INTO iam_department_leader(tenant_id,department_id,member_id,is_primary) VALUES(10,11,101,TRUE),(10,11,102,FALSE),(10,11,103,FALSE),(10,11,105,FALSE),(10,12,104,TRUE),(20,21,201,TRUE)");
        repository = new DepartmentLeaderRepository(IamMybatisTestAccess.mapper(source, IamDepartmentLeaderMapper.class),
                IamMybatisTestAccess.mapper(source, IamTenantMemberMapper.class));
    }

    @Test
    void boundPageRetainsUnavailableRelationsAndNeverCrossesTenant() {
        jdbc.update("UPDATE iam_tenant_member SET avatar='ingot/member/primary.png' WHERE id=101");
        jdbc.update("UPDATE iam_tenant_member SET avatar='ingot/member/other.png' WHERE id=102");
        var query = query(ObjectScope.all(), false);
        var rows = repository.page(query);
        assertEquals(4, repository.count(query));
        assertEquals(List.of(101L, 102L, 103L, 105L), rows.stream().map(row -> row.getMemberId().longValueExact()).toList());
        assertTrue(rows.getFirst().isPrimaryFlag());
        assertEquals("ingot/member/primary.png", rows.getFirst().getAvatar());
        var summary = repository.summaries(new DepartmentLeaderSql.Query(id(10), id(11), List.of(id(11)),
                ObjectScope.all(), null, 20, 0, false)).getFirst();
        assertEquals("ingot/member/primary.png", summary.getAvatar());
        assertTrue(rows.getFirst().isAvailable());
        assertTrue(rows.stream().skip(1).noneMatch(row -> row.isAvailable()));
        assertEquals(MemberStatus.REMOVED, rows.getLast().getStatus());
    }

    @Test
    void candidatesRequireActiveMemberAndAccountAndExcludeExistingBindings() {
        var query = query(ObjectScope.all(), true);
        assertEquals(List.of(104L, 106L), repository.page(query).stream().map(row -> row.getMemberId().longValueExact()).toList());
        assertEquals(2, repository.count(query));
    }

    @Test
    void scopeKeepsAndBetweenDepartmentSetsAndOrBetweenSources() {
        var scope = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(), List.of(Set.of(id(11)), Set.of(id(12))))));
        assertEquals(1, repository.count(query(scope, false)));
        assertEquals(id(101), repository.page(query(scope, false)).getFirst().getMemberId());
        var union = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(102)), List.of()),
                new ObjectScopeClause(Set.of(id(105)), List.of())));
        assertEquals(2, repository.count(query(union, false)));
        assertEquals(0, repository.count(query(ObjectScope.none(), false)));
        assertTrue(repository.visibleMember(id(10), id(105), union));
        assertFalse(repository.visibleMember(id(10), id(201), ObjectScope.all()));
    }

    @Test
    void summaryAndPaginationAreRestrictedBeforeCounting() {
        var scope = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(id(102)), List.of())));
        var query = new DepartmentLeaderSql.Query(id(10), id(11), List.of(id(11), id(12)), scope, null, 1, 0, false);
        var summaries = repository.summaries(query);
        assertEquals(1, summaries.size());
        assertEquals(1, summaries.getFirst().getVisibleCount());
        assertNull(summaries.getFirst().getMemberId());
        assertEquals(1, repository.page(query).size());
        assertEquals(1, repository.count(query));
    }

    @Test
    void saveAndAuditFailureRollBackTheSameTransaction() {
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            var before = Map.of(id(101), true, id(102), false, id(103), false, id(105), false);
            repository.save(id(10), id(11), before, Map.of(id(106), true), BigInteger.ZERO);
            throw new IllegalStateException("模拟审计失败");
        }));
        assertEquals(4, repository.relationCount(10, 11));
        assertEquals(0, jdbc.queryForObject("SELECT version FROM iam_department WHERE id=11", Integer.class));
        assertEquals(101L, jdbc.queryForObject("SELECT member_id FROM iam_department_leader WHERE department_id=11 AND is_primary=TRUE", Long.class));
    }

    @Test
    void pickerRootKeepsCandidateExclusionsAndCountsDualMembershipOnce() {
        jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name,status) VALUES(107,10,1,'零任职','ACTIVE'),(108,10,1,'根兼任','ACTIVE'),(109,10,1,'隐藏任职','ACTIVE')");
        jdbc.update("INSERT INTO iam_member_department VALUES(10,101,1),(10,106,1),(10,108,1),(10,108,12),(10,109,99)");
        var root=new DepartmentLeaderSql.Query(id(10),id(11),List.of(),ObjectScope.all(),null,1,0,true,null,com.ingot.cloud.iam.persistence.MemberPickerBranch.department(1,true));
        assertEquals(3,repository.count(root));
        assertEquals(List.of(id(106)),repository.page(root).stream().map(row->row.getMemberId()).toList());
        var branch=new DepartmentLeaderSql.Query(id(10),id(11),List.of(),ObjectScope.all(),null,20,0,true,null,com.ingot.cloud.iam.persistence.MemberPickerBranch.department(12,false));
        assertEquals(List.of(id(104),id(108)),repository.page(branch).stream().map(row->row.getMemberId()).toList());
        assertEquals(4,repository.count(query(ObjectScope.all(),false)));
    }
    private static DepartmentLeaderSql.Query query(ObjectScope scope, boolean candidates) {
        return new DepartmentLeaderSql.Query(id(10), id(11), List.of(id(11)), scope, null, 20, 0, candidates);
    }
    private static BigInteger id(long value) { return BigInteger.valueOf(value); }
}
