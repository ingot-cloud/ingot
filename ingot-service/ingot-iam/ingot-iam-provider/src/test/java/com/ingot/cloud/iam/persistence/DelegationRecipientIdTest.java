package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.UUID;

import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.TenantLiveMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>通过真实MyBatis验证两域接收成员显式ID写入、唯一性及事务回滚。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DelegationRecipientIdTest {
    private JdbcTemplate jdbc;
    private IamDelegationRecipientMemberMapper platform;
    private TenantLiveMapper tenant;
    private TransactionTemplate transactions;

    @BeforeEach
    void database() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(id DECIMAL(20,0) NOT NULL PRIMARY KEY,"
                + "delegation_id BIGINT NOT NULL,domain VARCHAR(16),tenant_id BIGINT,platform_member_id BIGINT,"
                + "tenant_member_id BIGINT,UNIQUE(delegation_id,platform_member_id),UNIQUE(delegation_id,tenant_member_id))");
        platform = IamMybatisTestAccess.mapper(source, IamDelegationRecipientMemberMapper.class);
        tenant = IamMybatisTestAccess.mapper(source, TenantLiveMapper.class);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    @Test
    void platformInputPreservesTheAllocatedBigIntegerAndNeverGeneratesAnId() {
        var row = platformRow("900001", "31");
        assertEquals(1, platform.insert(row));
        assertEquals(new BigInteger("900001"), row.getId());
        assertEquals(row.getId(), platform.selectById(row.getId()).getId());
        var missing = platformRow(null, "32");
        assertThrows(RuntimeException.class, () -> platform.insert(missing));
        assertNull(missing.getId());
    }

    @Test
    void tenantCustomInsertUsesItsAllocatedIdAndKeepsOwnershipColumns() {
        assertEquals(1, tenant.recipientMember(new BigInteger("900002"), BigInteger.TEN,
                new BigInteger("60"), new BigInteger("102")));
        assertEquals("900002", jdbc.queryForObject("SELECT CAST(id AS VARCHAR) FROM iam_delegation_recipient_member", String.class));
        assertEquals(new BigInteger("102"), tenant.recipientMembers(BigInteger.TEN, new BigInteger("60")).getFirst());
        assertTrue(tenant.recipientMembers(new BigInteger("20"), new BigInteger("60")).isEmpty());
    }

    @Test
    void duplicateKeysRejectAndBothMapperPathsRollbackTogether() {
        platform.insert(platformRow("900001", "31"));
        assertThrows(RuntimeException.class, () -> platform.insert(platformRow("900002", "31")));
        assertThrows(RuntimeException.class, () -> transactions.executeWithoutResult(status -> {
            tenant.recipientMember(new BigInteger("900003"), BigInteger.TEN, new BigInteger("60"), new BigInteger("102"));
            platform.insert(platformRow("900004", "32"));
            throw new IllegalStateException("fixture audit failure");
        }));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_delegation_recipient_member", Integer.class));
    }

    private IamDelegationRecipientMemberEntity platformRow(String id, String member) {
        var row = new IamDelegationRecipientMemberEntity();
        row.setId(id == null ? null : new BigInteger(id));
        row.setDelegationId(new BigInteger("50"));
        row.setDomain(AuthorizationDomain.PLATFORM);
        row.setPlatformMemberId(new BigInteger(member));
        return row;
    }
}
