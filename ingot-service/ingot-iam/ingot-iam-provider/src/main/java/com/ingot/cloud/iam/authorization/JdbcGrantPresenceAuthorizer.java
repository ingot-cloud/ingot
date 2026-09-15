package com.ingot.cloud.iam.authorization;

import java.util.Map;
import javax.sql.DataSource;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.SubjectType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>按当前成员的有效直接分配检查 ACTION 是否出现在角色版本授权中。</p>
 *
 * <p>这是 T05 写入路径的失败关闭门闩：不计算部门范围、组主体、租户差异或委派上限。完整求值仍由 T09 引擎承担。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcGrantPresenceAuthorizer implements IamActionAuthorizer {
    private static final String PLATFORM_GRANT = """
            SELECT COUNT(*) FROM iam_role_assignment ra
              JOIN iam_role_grant g ON g.revision_id = ra.revision_id
              JOIN iam_action a ON a.id = g.action_id
             WHERE ra.domain = :domain AND ra.status = :active
               AND ra.subject_type = :member AND ra.platform_member_id = :memberId
               AND ra.tenant_id IS NULL
               AND (ra.valid_until IS NULL OR ra.valid_until > CURRENT_TIMESTAMP)
               AND a.enabled = TRUE AND a.code = :action
            """;
    private static final String TENANT_GRANT = """
            SELECT COUNT(*) FROM iam_role_assignment ra
              JOIN iam_role_grant g ON g.revision_id = ra.revision_id
              JOIN iam_action a ON a.id = g.action_id
             WHERE ra.domain = :domain AND ra.status = :active
               AND ra.subject_type = :member AND ra.tenant_member_id = :memberId
               AND ra.tenant_id = :tenantId
               AND (ra.valid_until IS NULL OR ra.valid_until > CURRENT_TIMESTAMP)
               AND a.enabled = TRUE AND a.code = :action
            """;
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * 绑定 IAM 目标库。
     *
     * @param dataSource 独立 IAM 数据源
     */
    public JdbcGrantPresenceAuthorizer(DataSource dataSource) {
        jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void require(AuthorizationContext actor, IamAction action) {
        if (actor == null || action == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        try {
            if (count(actor, action) < 1) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
        } catch (DataAccessException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
    }

    private long count(AuthorizationContext actor, IamAction action) {
        var parameters = new java.util.HashMap<String, Object>(Map.of(
                "domain", actor.domain().name(),
                "active", GrantStatus.ACTIVE.name(),
                "member", SubjectType.MEMBER.name(),
                "memberId", Long.parseLong(actor.memberId()),
                "action", action.getCode()));
        String sql = PLATFORM_GRANT;
        if (actor.domain() == AuthorizationDomain.TENANT) {
            parameters.put("tenantId", Long.parseLong(actor.tenantId()));
            sql = TENANT_GRANT;
        }
        Long count = jdbc.queryForObject(sql, parameters, Long.class);
        return count == null ? 0 : count;
    }
}
