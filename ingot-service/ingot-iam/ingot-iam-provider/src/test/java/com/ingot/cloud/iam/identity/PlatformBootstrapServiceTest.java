package com.ingot.cloud.iam.identity;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;

import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.inbound.RegisterUserUseCase;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证冷启动建立首个平台身份、写入新模型账号、幂等以及缺少治理版本时的失败。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class PlatformBootstrapServiceTest {
    private static final String GENERATED_PASSWORD = "generated-initial-password";
    private static final long FIRST_ID = 1000101L;

    private JdbcTemplate jdbc;
    private AccountWriteRepository accountWrites;
    private RecordingInitialPassword initialPassword;
    private IamBootstrapProperties properties;
    private PlatformBootstrapService bootstrap;

    @BeforeEach
    void database() {
        DataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("identity/bootstrap.sql")).execute(source);
        jdbc = new JdbcTemplate(source);
        InitializationIdAllocator ids = sequentialIds();
        accountWrites = IamMybatisTestAccess.accountWrites(source, ids);
        initialPassword = new RecordingInitialPassword();
        properties = new IamBootstrapProperties();
        bootstrap = new PlatformBootstrapService(properties,
                IamMybatisTestAccess.platformBootstrap(source),
                IamMybatisTestAccess.catalog(source),
                IamMybatisTestAccess.memberQueries(source),
                ids, new AdminCreateRegisterUser(accountWrites), initialPassword);
    }

    @Test
    void emptyDatabaseGetsPlatformAccountMemberAndGovernanceAssignment() {
        bootstrap.initialize();

        assertEquals(1, count("iam_account"));
        assertEquals("platform", jdbc.queryForObject("SELECT username FROM iam_account", String.class));
        assertEquals(GENERATED_PASSWORD, initialPassword.issued);
        // 初始改密由框架的初始密码策略决定，冷启动不自行判定。
        assertTrue(jdbc.queryForObject("SELECT must_change_password FROM iam_account", Boolean.class));
        assertTrue(jdbc.queryForObject("SELECT enabled FROM iam_account", Boolean.class));
        assertNotNull(jdbc.queryForObject("SELECT password_changed_at FROM iam_account", LocalDateTime.class));
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_account", Long.class));

        assertEquals("平台治理",
                jdbc.queryForObject("SELECT display_name FROM iam_platform_member", String.class));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_platform_member", String.class));

        assertEquals(1, count("iam_role_assignment"));
        assertEquals("PLATFORM", jdbc.queryForObject("SELECT domain FROM iam_role_assignment", String.class));
        assertEquals("MEMBER", jdbc.queryForObject("SELECT subject_type FROM iam_role_assignment", String.class));
        assertEquals("INITIALIZATION", jdbc.queryForObject("SELECT source FROM iam_role_assignment", String.class));
        assertEquals("{}", jdbc.queryForObject("SELECT scope_bindings FROM iam_role_assignment", String.class));
        // 授权引用平台域系统角色的最新版本，不引用租户治理版本。
        assertEquals(141001L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment", Long.class));
        assertEquals(jdbc.queryForObject("SELECT id FROM iam_platform_member", Long.class),
                jdbc.queryForObject("SELECT platform_member_id FROM iam_role_assignment", Long.class));
    }

    @Test
    void repeatedRunKeepsTheExistingIdentityAndIssuesNoPassword() {
        bootstrap.initialize();
        long accountId = jdbc.queryForObject("SELECT id FROM iam_account", Long.class);
        initialPassword.issued = null;

        bootstrap.initialize();

        assertEquals(1, count("iam_account"));
        assertEquals(1, count("iam_platform_member"));
        assertEquals(1, count("iam_role_assignment"));
        assertEquals(accountId, jdbc.queryForObject("SELECT id FROM iam_account", Long.class));
        assertEquals(null, initialPassword.issued);
    }

    @Test
    void existingAccountIsReusedInsteadOfCreatingADuplicateLogin() {
        UserAccount existing = UserAccount.builder().username("platform").password("already-encoded").build();
        long accountId = accountWrites.insert(existing);
        initialPassword.issued = null;

        bootstrap.initialize();

        assertEquals(1, count("iam_account"));
        assertEquals(null, initialPassword.issued);
        assertEquals("already-encoded",
                jdbc.queryForObject("SELECT password_hash FROM iam_account", String.class));
        assertEquals(accountId,
                jdbc.queryForObject("SELECT account_id FROM iam_platform_member", Long.class));
        assertEquals(1, count("iam_role_assignment"));
    }

    @Test
    void configuredLoginNameAndDisplayNameAreUsed() {
        properties.setUsername("ops-admin");
        properties.setDisplayName("运维治理");
        properties.setPhone("13800000001");
        properties.setEmail("ops@example.com");

        bootstrap.initialize();

        assertEquals("ops-admin", jdbc.queryForObject("SELECT username FROM iam_account", String.class));
        assertEquals("13800000001", jdbc.queryForObject("SELECT phone FROM iam_account", String.class));
        assertEquals("ops@example.com", jdbc.queryForObject("SELECT email FROM iam_account", String.class));
        assertEquals("运维治理",
                jdbc.queryForObject("SELECT display_name FROM iam_platform_member", String.class));
    }

    @Test
    void missingGovernanceRevisionFailsBeforeAnyIdentityIsCreated() {
        jdbc.update("DELETE FROM iam_role_revision WHERE id = 141001");

        assertThrows(IllegalStateException.class, bootstrap::initialize);

        assertEquals(0, count("iam_account"));
        assertEquals(0, count("iam_platform_member"));
        assertEquals(0, count("iam_role_assignment"));
        assertEquals(null, initialPassword.issued);
    }

    @Test
    void ambiguousPlatformGovernanceRoleFailsBeforeAnyIdentityIsCreated() {
        jdbc.update("INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name) "
                + "VALUES (140003,'PLATFORM',NULL,'SYSTEM','second-governance','另一个平台治理')");

        assertThrows(IllegalStateException.class, bootstrap::initialize);

        assertEquals(0, count("iam_account"));
        assertEquals(0, count("iam_platform_member"));
        assertEquals(0, count("iam_role_assignment"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private static InitializationIdAllocator sequentialIds() {
        AtomicLong next = new AtomicLong(FIRST_ID);
        return next::getAndIncrement;
    }

    /** 只复现真实注册用例中与账号持久化相关的部分，凭证策略与事件由框架自身测试覆盖。 */
    private record AdminCreateRegisterUser(AccountWriteRepository accounts) implements RegisterUserUseCase {
        @Override
        public UserAccount register(RegisterUserCommand command) {
            UserAccount account = UserAccount.builder()
                    .username(command.getUsername())
                    .password("encoded:" + command.getPassword())
                    .phone(command.getPhone())
                    .email(command.getEmail())
                    .mustChangePwd(Boolean.TRUE)
                    .enabled(true)
                    .build();
            accounts.insert(account);
            return account;
        }
    }

    /** 记录本次是否真的向策略申请过初始口令。 */
    private static final class RecordingInitialPassword implements InitialPasswordService {
        private String issued;

        @Override
        public String generate() {
            issued = GENERATED_PASSWORD;
            return GENERATED_PASSWORD;
        }

        @Override
        public boolean isExpired(LocalDateTime issuedAt) {
            return false;
        }

        @Override
        public boolean isForceChangeOnFirstLogin() {
            return true;
        }
    }
}
