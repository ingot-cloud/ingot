package com.ingot.cloud.iam.organization;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.evaluation.ResourceScopeFilter;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberProfileInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>查询并维护当前域成员资格，按字段策略投影资料并拒绝不可编辑字段写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class MemberQueryService {
    private final IamAccess access;
    private final ResourceAccess scopes;
    private final FieldAccessEvaluator fields;
    private final IamAuditWriter audits;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、范围、字段策略与成员表。
     *
     * @param access 当前身份
     * @param scopes 对象范围
     * @param fields 字段访问
     * @param audits 同事务审计
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public MemberQueryService(IamAccess access, ResourceAccess scopes, FieldAccessEvaluator fields,
                              IamAuditWriter audits, DataSource dataSource,
                              PlatformTransactionManager transactionManager) {
        this.access = access;
        this.scopes = scopes;
        this.fields = fields;
        this.audits = audits;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域成员。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return list(domain, page, pageSize, null, null);
    }

    /**
     * 分页列出当前域成员；原值筛选仅在字段完整可见时允许。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> list(AuthorizationDomain domain, int page, int pageSize,
                                                           String phone, String email) {
        IamAction action = domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
        ActiveIdentity actor = access.require(domain, action);
        if (domain == AuthorizationDomain.PLATFORM && (phone != null || email != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return listProjected(actor, action, page, pageSize, phone, email);
    }

    /**
     * 已通过 ACTION 校验后按范围与字段策略列出成员，供导出下载重验复用。
     *
     * @param actor 当前身份
     * @param action 列表或导出操作
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 投影后的成员页
     */
    public PageResponse<ResourceDetail<MemberRecord>> listProjected(ActiveIdentity actor, IamAction action,
                                                                    int page, int pageSize, String phone,
                                                                    String email) {
        IamPages.require(page, pageSize);
        AuthorizationDomain domain = actor.context().domain();
        ResourceScopeFilter.Predicate scope = scopes.memberRead(actor.context(), action, "id");
        Map<String, Object> parameters = new HashMap<>(scope.parameters());
        parameters.put("limit", pageSize);
        parameters.put("offset", IamPages.offset(page, pageSize));
        if (domain == AuthorizationDomain.PLATFORM) {
            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_platform_member WHERE status<>'REMOVED' AND " + scope.sql(),
                    scope.parameters(), Long.class);
            List<ResourceDetail<MemberRecord>> items = jdbc.query("""
                    SELECT id,display_name,avatar,status,version FROM iam_platform_member
                     WHERE status<>'REMOVED' AND %s ORDER BY id LIMIT :limit OFFSET :offset
                    """.formatted(scope.sql()), parameters,
                    (row, index) -> IamDetails.of(platformMember(row), row.getString("version")));
            return IamPages.details(items, total == null ? 0 : total, page, pageSize);
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        fields.requireOriginalLookup(tenantId, viewerId, MemberFieldKey.VALUE_PHONE, phone);
        fields.requireOriginalLookup(tenantId, viewerId, MemberFieldKey.VALUE_EMAIL, email);
        parameters.put("tenantId", tenantId);
        Map<String, Object> countParameters = new HashMap<>(scope.parameters());
        countParameters.put("tenantId", tenantId);
        String filters = "";
        if (phone != null && !phone.isBlank()) {
            parameters.put("phone", phone);
            countParameters.put("phone", phone);
            filters += " AND phone=:phone";
        }
        if (email != null && !email.isBlank()) {
            parameters.put("email", email);
            countParameters.put("email", email);
            filters += " AND email=:email";
        }
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_tenant_member WHERE tenant_id=:tenantId AND status<>'REMOVED' AND "
                        + scope.sql() + filters, countParameters, Long.class);
        List<ResourceDetail<MemberRecord>> items = jdbc.query("""
                SELECT id,display_name,avatar,phone,email,status,version FROM iam_tenant_member
                 WHERE tenant_id=:tenantId AND status<>'REMOVED' AND %s%s
                 ORDER BY id LIMIT :limit OFFSET :offset
                """.formatted(scope.sql(), filters), parameters,
                (row, index) -> detail(tenantId, viewerId, tenantMember(tenantId, row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    /**
     * 读取当前域成员详情。
     *
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @return 安全投影
     */
    public ResourceDetail<MemberRecord> get(AuthorizationDomain domain, String memberId) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ);
        long id = IamIds.require(memberId);
        scopes.requireVisibleMember(actor.context(),
                domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ,
                id);
        return load(domain, actor, id);
    }

    /**
     * 把已有账号关联为当前域成员。
     *
     * @param domain 接口管理域
     * @param input 账号与任职
     * @return 新成员 ID
     */
    public CreatedResource create(AuthorizationDomain domain, MemberCreateInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_CREATE : IamAction.TENANT_MEMBER_CREATE);
        if (domain == AuthorizationDomain.PLATFORM && !input.departments().isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Set<String> departmentIds = new LinkedHashSet<>();
        if (input.departments() != null) {
            for (MemberDepartmentBinding binding : input.departments()) {
                departmentIds.add(binding.id());
            }
        }
        scopes.requireCreate(actor.context(),
                domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_CREATE
                        : IamAction.TENANT_MEMBER_CREATE, departmentIds);
        long accountId = IamIds.require(input.accountId());
        return transaction.execute(status -> {
            Long accounts = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_account WHERE id=:id AND enabled=TRUE AND deleted_at IS NULL",
                    Map.of("id", accountId), Long.class);
            if (accounts == null || accounts != 1) {
                throw new BizException(IamReasonCode.IDENTITY_INVALID);
            }
            long id = access.nextId();
            String displayName = input.displayName() == null || input.displayName().isBlank()
                    ? "成员" : input.displayName().trim();
            try {
                if (domain == AuthorizationDomain.PLATFORM) {
                    jdbc.update("""
                            INSERT INTO iam_platform_member(id,account_id,display_name,status)
                            VALUES (:id,:accountId,:displayName,:status)
                            """, Map.of("id", id, "accountId", accountId, "displayName", displayName,
                            "status", MemberStatus.ACTIVE.name()));
                } else {
                    long tenantId = IamIds.require(actor.context().tenantId());
                    jdbc.update("""
                            INSERT INTO iam_tenant_member(id,tenant_id,account_id,display_name,status)
                            VALUES (:id,:tenantId,:accountId,:displayName,:status)
                            """, Map.of("id", id, "tenantId", tenantId, "accountId", accountId,
                            "displayName", displayName, "status", MemberStatus.ACTIVE.name()));
                    replaceDepartments(tenantId, id, input.departments());
                }
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            audits.write(actor.context(), access.nextId(), "member", IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, displayName), Map.of("member", "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新当前域显示资料，不修改凭证或状态；不可编辑字段拒绝写入。
     *
     * @param domain 接口管理域
     * @param memberId 成员 ID
     * @param input 资料
     * @return 更新后投影
     */
    public ResourceDetail<MemberRecord> patch(AuthorizationDomain domain, String memberId, MemberProfileInput input) {
        ActiveIdentity actor = access.require(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_MEMBER_UPDATE : IamAction.TENANT_MEMBER_UPDATE);
        long id = IamIds.require(memberId);
        scopes.requireVisibleMember(actor.context(),
                domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_UPDATE
                        : IamAction.TENANT_MEMBER_UPDATE, id);
        if (domain == AuthorizationDomain.PLATFORM && (input.phone() != null || input.email() != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return transaction.execute(status -> {
            ResourceDetail<MemberRecord> current = load(domain, actor, id);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", id);
            parameters.put("displayName", input.displayName());
            parameters.put("avatar", input.avatar());
            if (domain == AuthorizationDomain.PLATFORM) {
                jdbc.update("""
                        UPDATE iam_platform_member
                           SET display_name=COALESCE(:displayName,display_name),
                               avatar=COALESCE(:avatar,avatar),version=version+1
                         WHERE id=:id AND status<>'REMOVED'
                        """, parameters);
            } else {
                long tenantId = IamIds.require(actor.context().tenantId());
                long viewerId = IamIds.require(actor.context().memberId());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_DISPLAY_NAME, input.displayName());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_AVATAR, input.avatar());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_PHONE, input.phone());
                fields.requireWritable(tenantId, viewerId, id, MemberFieldKey.VALUE_EMAIL, input.email());
                parameters.put("tenantId", tenantId);
                parameters.put("phone", input.phone());
                parameters.put("email", input.email());
                parameters.put("phoneSet", input.phone() != null);
                parameters.put("emailSet", input.email() != null);
                jdbc.update("""
                        UPDATE iam_tenant_member
                           SET display_name=COALESCE(:displayName,display_name),
                               avatar=COALESCE(:avatar,avatar),
                               phone=CASE WHEN :phoneSet THEN :phone ELSE phone END,
                               email=CASE WHEN :emailSet THEN :email ELSE email END,
                               version=version+1
                         WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                        """, parameters);
            }
            ResourceDetail<MemberRecord> next = load(domain, actor, id);
            audits.write(actor.context(), access.nextId(), "member", memberId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, String.valueOf(current.record().displayName())),
                    Map.of(AuditField.NAME, String.valueOf(next.record().displayName())),
                    Map.of("member", next.version()));
            return next;
        });
    }

    private ResourceDetail<MemberRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long memberId) {
        if (domain == AuthorizationDomain.PLATFORM) {
            List<ResourceDetail<MemberRecord>> rows = jdbc.query("""
                    SELECT id,display_name,avatar,status,version FROM iam_platform_member
                     WHERE id=:id AND status<>'REMOVED'
                    """, Map.of("id", memberId),
                    (row, index) -> IamDetails.of(platformMember(row), row.getString("version")));
            if (rows.size() != 1) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return rows.getFirst();
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        long viewerId = IamIds.require(actor.context().memberId());
        List<ResourceDetail<MemberRecord>> rows = jdbc.query("""
                SELECT id,display_name,avatar,phone,email,status,version FROM iam_tenant_member
                 WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                """, Map.of("tenantId", tenantId, "id", memberId),
                (row, index) -> detail(tenantId, viewerId, tenantMember(tenantId, row), row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return rows.getFirst();
    }

    private ResourceDetail<MemberRecord> detail(long tenantId, long viewerId, MemberRecord raw, String version) {
        Map<String, FieldAccess> access = fields.memberAccess(tenantId, viewerId, IamIds.require(raw.id()),
                PolicyScenario.MANAGEMENT);
        return IamDetails.of(fields.project(raw, access), access, Map.of(), version);
    }

    private MemberRecord platformMember(java.sql.ResultSet row) throws java.sql.SQLException {
        return new MemberRecord(row.getString("id"), row.getString("display_name"), row.getString("avatar"),
                null, null, MemberStatus.valueOf(row.getString("status")), List.of());
    }

    private MemberRecord tenantMember(long tenantId, java.sql.ResultSet row) throws java.sql.SQLException {
        List<MemberDepartmentView> departments = jdbc.query("""
                SELECT d.id,d.name,md.is_primary
                  FROM iam_member_department md JOIN iam_department d
                    ON d.id=md.department_id AND d.tenant_id=md.tenant_id
                 WHERE md.tenant_id=:tenantId AND md.member_id=:memberId
                 ORDER BY md.is_primary DESC,d.id
                """, Map.of("tenantId", tenantId, "memberId", row.getLong("id")),
                (item, index) -> new MemberDepartmentView(item.getString("id"), item.getString("name"),
                        item.getBoolean("is_primary")));
        return new MemberRecord(row.getString("id"), row.getString("display_name"), row.getString("avatar"),
                row.getString("phone"), row.getString("email"), MemberStatus.valueOf(row.getString("status")),
                departments);
    }

    private void replaceDepartments(long tenantId, long memberId, List<MemberDepartmentBinding> departments) {
        jdbc.update("DELETE FROM iam_member_department WHERE tenant_id=:tenantId AND member_id=:memberId",
                Map.of("tenantId", tenantId, "memberId", memberId));
        if (departments == null) {
            return;
        }
        for (MemberDepartmentBinding binding : departments) {
            long departmentId = IamIds.require(binding.id());
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", tenantId, "id", departmentId), Long.class);
            if (count == null || count != 1) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            jdbc.update("""
                    INSERT INTO iam_member_department(tenant_id,member_id,department_id,is_primary)
                    VALUES (:tenantId,:memberId,:departmentId,:primary)
                    """, Map.of("tenantId", tenantId, "memberId", memberId, "departmentId", departmentId,
                    "primary", binding.primary()));
        }
    }
}
