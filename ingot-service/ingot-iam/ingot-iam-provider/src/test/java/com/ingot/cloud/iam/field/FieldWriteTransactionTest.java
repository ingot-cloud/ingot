package com.ingot.cloud.iam.field;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.authorization.field.*;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>真实业务事务中重新求值；撤权导致回滚，脱敏可写 null 保持显式清空语义。</p>
 * @author jy
 * @since 1.0.0
 */
class FieldWriteTransactionTest {
    private static final ResourceKey KEY = MemberResources.PLATFORM_MEMBER;
    private static final String UPDATE = IamAction.VALUE_PLATFORM_MEMBER_UPDATE, PHONE = MemberFieldKey.VALUE_PHONE;

    @Test
    void previewCannotAuthorizeRevokedWriteAndRejectionRollsBack() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE target(id INT PRIMARY KEY, phone VARCHAR(64))");
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        var granted = new AtomicBoolean(true);
        var calls = new AtomicInteger();
        FieldPolicyProvider provider = new FieldPolicyProvider() {
            @Override public AuthorizationDecision read(ResourceKey resource, String action) { return decision(true); }
            @Override public AuthorizationDecision write(ResourceKey resource, String action) { calls.incrementAndGet(); return decision(granted.get()); }
        };
        var executor = new FieldWriteExecutor(provider);
        var target = new ScopeTarget("1", "99", null, List.of());
        Map<String, Object> clear = new HashMap<>(); clear.put(PHONE, null);
        assertThrows(IllegalStateException.class, () -> executor.require(KEY, UPDATE, target, clear));
        var preview = provider.read(KEY, UPDATE);
        assertTrue(preview.actions().get(UPDATE).allowed());
        granted.set(false);
        assertThrows(SdkAuthorizationException.class, () -> transaction.execute(status -> {
            jdbc.update("INSERT INTO target VALUES(1,'before')");
            executor.require(KEY, UPDATE, target, clear);
            return null;
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM target", Integer.class));
        granted.set(true);
        transaction.execute(status -> {
            executor.require(KEY, UPDATE, target, clear);
            jdbc.update("INSERT INTO target VALUES(1,NULL)"); return null;
        });
        assertNull(jdbc.queryForObject("SELECT phone FROM target WHERE id=1", String.class));
        assertEquals(2, calls.get());
    }

    private static AuthorizationDecision decision(boolean allowed) {
        FieldAccess hidden = new FieldAccess(FieldVisibility.HIDDEN, false), masked = new FieldAccess(FieldVisibility.MASKED, true);
        var scope = List.of(new ScopeCondition(true, List.of(), null, List.of()));
        var policy = new FieldPolicyDecision(Map.of(PHONE, hidden), Map.of(PHONE, new FieldAccess(FieldVisibility.FULL, true)),
                List.of(new ResolvedFieldRule(PHONE, scope, masked)), Map.of(PHONE, new FieldOperations(true, false)),
                Map.of(PHONE, MaskSpec.PHONE), FieldMergeMode.GRANTS);
        return new AuthorizationDecision(KEY, new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "9", "99"),
                Map.of(UPDATE, new ActionDecision(allowed, true, scope, policy)), "1", Instant.now().plusSeconds(30));
    }
}
