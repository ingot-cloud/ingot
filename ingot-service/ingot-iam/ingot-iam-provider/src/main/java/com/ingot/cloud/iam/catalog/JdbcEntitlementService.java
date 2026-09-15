package com.ingot.cloud.iam.catalog;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionRecord;
import com.ingot.framework.commons.model.iam.AudienceDraft;
import com.ingot.framework.commons.model.iam.AudienceKind;
import com.ingot.framework.commons.model.iam.AudienceUpdateInput;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.EntitlementDraft;
import com.ingot.framework.commons.model.iam.EntitlementPreviewResult;
import com.ingot.framework.commons.model.iam.EntitlementRecord;
import com.ingot.framework.commons.model.iam.EntitlementReplaceInput;
import com.ingot.framework.commons.model.iam.EntitlementSource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护租户显式开通与可用人群，开通不授予业务操作，停用强于授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class JdbcEntitlementService {
    private static final String ENTITLEMENT = "entitlement";
    private static final String AUDIENCE = "audience";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计与目录库。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 开通与人群变更后的授权失效
     * @param dataSource IAM 目标库
     * @param transactionManager 同一数据源事务
     */
    public JdbcEntitlementService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                  DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 平台读取指定组织的开通清单。
     *
     * @param tenantId 组织 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 开通页
     */
    public PageResponse<ResourceDetail<EntitlementRecord>> listForPlatform(String tenantId, int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ENTITLEMENT_READ);
        return list(IamIds.require(tenantId), page, pageSize);
    }

    /**
     * 当前租户读取已开通应用。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 开通页
     */
    public PageResponse<ResourceDetail<EntitlementRecord>> listForTenant(int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_APPLICATION_READ);
        return list(IamIds.require(actor.context().tenantId()), page, pageSize);
    }

    /**
     * 当前租户读取已开通应用的操作候选，不授予这些操作。
     *
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 操作页
     */
    public PageResponse<ResourceDetail<ActionRecord>> listTenantApplicationActions(String applicationId,
                                                                                   int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_APPLICATION_READ);
        long tenantId = IamIds.require(actor.context().tenantId());
        long appId = IamIds.require(applicationId);
        if (!entitled(tenantId, appId)) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        IamPages.require(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_action WHERE application_id=:id",
                Map.of("id", appId), Long.class);
        List<ResourceDetail<ActionRecord>> items = jdbc.query("""
                SELECT id,application_id,resource_id,code,name,enabled,version
                  FROM iam_action WHERE application_id=:id ORDER BY id LIMIT :limit OFFSET :offset
                """, Map.of("id", appId, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(new ActionRecord(row.getString("id"), row.getString("application_id"),
                        row.getString("resource_id"), row.getString("code"), row.getString("name"),
                        row.getBoolean("enabled") ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED),
                        row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    /**
     * 预览开通替换，无写入。
     *
     * @param tenantId 组织 ID
     * @param input 完整开通清单
     * @return 只读预览
     */
    public Preview<EntitlementPreviewResult> preview(String tenantId, EntitlementReplaceInput input) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ENTITLEMENT_PREVIEW);
        long id = IamIds.require(tenantId);
        requireTenant(id);
        List<ValidationIssue> errors = validateReplace(id, input, false);
        if (!input.expectedVersion().equals(collectionVersion(id))) {
            errors = new ArrayList<>(errors);
            errors.add(new ValidationIssue("expectedVersion", IamReasonCode.REVISION_CONFLICT, "开通配置已变化，请重新预览后提交"));
        }
        EntitlementPreviewResult result = new EntitlementPreviewResult(input.entitlements(),
                new ImpactSummary(null, null, null, false));
        return new Preview<>(collectionVersion(id), errors.isEmpty(), errors, List.of(),
                result.impactSummary(), result);
    }

    /**
     * 整体替换组织开通；基础治理应用不可关闭。
     *
     * @param tenantId 组织 ID
     * @param input 完整清单与版本
     * @return 替换后的开通页
     */
    public PageResponse<ResourceDetail<EntitlementRecord>> replace(String tenantId, EntitlementReplaceInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ENTITLEMENT_UPDATE);
        long id = IamIds.require(tenantId);
        return transaction.execute(status -> {
            requireTenant(id);
            IamIds.requireExpected(input.expectedVersion(), collectionVersion(id));
            List<ValidationIssue> errors = validateReplace(id, input, true);
            if (!errors.isEmpty()) {
                throw new BizException(errors.getFirst().code());
            }
            jdbc.update("DELETE FROM iam_audience_member WHERE tenant_id=:tenantId", Map.of("tenantId", id));
            jdbc.update("DELETE FROM iam_audience_department WHERE tenant_id=:tenantId", Map.of("tenantId", id));
            jdbc.update("DELETE FROM iam_audience_group WHERE tenant_id=:tenantId", Map.of("tenantId", id));
            jdbc.update("DELETE FROM iam_app_audience WHERE tenant_id=:tenantId", Map.of("tenantId", id));
            jdbc.update("DELETE FROM iam_tenant_app_entitlement WHERE tenant_id=:tenantId", Map.of("tenantId", id));
            Timestamp now = Timestamp.from(Instant.now());
            for (EntitlementDraft draft : input.entitlements()) {
                long applicationId = IamIds.require(draft.applicationId());
                long entitlementId = access.nextId();
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("id", entitlementId);
                parameters.put("tenantId", id);
                parameters.put("applicationId", applicationId);
                parameters.put("enabled", draft.status() == ConfigurationStatus.ENABLED);
                parameters.put("source", EntitlementSource.MANUAL.name());
                parameters.put("validFrom", draft.validFrom() == null ? now : Timestamp.from(draft.validFrom()));
                parameters.put("validUntil", draft.validUntil() == null ? null : Timestamp.from(draft.validUntil()));
                jdbc.update("""
                        INSERT INTO iam_tenant_app_entitlement
                          (id,tenant_id,application_id,enabled,source,valid_from,valid_until)
                        VALUES (:id,:tenantId,:applicationId,:enabled,:source,:validFrom,:validUntil)
                        """, parameters);
                jdbc.update("INSERT INTO iam_app_audience(tenant_id,application_id,audience_kind) VALUES (:tenantId,:applicationId,:kind)",
                        Map.of("tenantId", id, "applicationId", applicationId, "kind", AudienceKind.ALL.name()));
            }
            audits.write(actor.context(), access.nextId(), ENTITLEMENT, tenantId, AuditChangeType.UPDATE,
                    Map.of(), Map.of(AuditField.ENTITLEMENT, Integer.toString(input.entitlements().size())),
                    Map.of(ENTITLEMENT, collectionVersion(id)));
            changes.markAll();
            return list(id, IamPages.DEFAULT_PAGE, IamPages.MAX_SIZE);
        });
    }

    /**
     * 读取应用可用人群。
     *
     * @param applicationId 应用 ID
     * @return 人群配置
     */
    public ResourceDetail<AudienceDraft> getAudience(String applicationId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_AUDIENCE_READ);
        long tenantId = IamIds.require(actor.context().tenantId());
        long appId = IamIds.require(applicationId);
        return loadAudience(tenantId, appId);
    }

    /**
     * 替换应用可用人群；ALL 不得携带选择器或组。
     *
     * @param applicationId 应用 ID
     * @param input 完整人群与版本
     * @return 更新后配置
     */
    public ResourceDetail<AudienceDraft> putAudience(String applicationId, AudienceUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_AUDIENCE_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        long appId = IamIds.require(applicationId);
        AudienceDraft audience = input.audience();
        if (audience.kind() == AudienceKind.ALL
                && (audience.selection() != null || (audience.groupIds() != null && !audience.groupIds().isEmpty()))) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return transaction.execute(status -> {
            if (!entitled(tenantId, appId)) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
            ResourceDetail<AudienceDraft> current = loadAudience(tenantId, appId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            jdbc.update("DELETE FROM iam_audience_member WHERE tenant_id=:tenantId AND application_id=:applicationId",
                    Map.of("tenantId", tenantId, "applicationId", appId));
            jdbc.update("DELETE FROM iam_audience_department WHERE tenant_id=:tenantId AND application_id=:applicationId",
                    Map.of("tenantId", tenantId, "applicationId", appId));
            jdbc.update("DELETE FROM iam_audience_group WHERE tenant_id=:tenantId AND application_id=:applicationId",
                    Map.of("tenantId", tenantId, "applicationId", appId));
            jdbc.update("""
                    UPDATE iam_app_audience SET audience_kind=:kind,enabled=TRUE,version=version+1
                     WHERE tenant_id=:tenantId AND application_id=:applicationId
                    """, Map.of("kind", audience.kind().name(), "tenantId", tenantId, "applicationId", appId));
            if (audience.kind() == AudienceKind.SELECTED) {
                writeSelection(tenantId, appId, audience);
            }
            audits.write(actor.context(), access.nextId(), AUDIENCE, applicationId, AuditChangeType.UPDATE,
                    Map.of(), Map.of(AuditField.SCOPE, audience.kind().name()), Map.of(AUDIENCE, nextVersion(current.version())));
            changes.markAll();
            return loadAudience(tenantId, appId);
        });
    }

    private PageResponse<ResourceDetail<EntitlementRecord>> list(long tenantId, int page, int pageSize) {
        requireTenant(tenantId);
        IamPages.require(page, pageSize);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_app_entitlement WHERE tenant_id=:tenantId",
                Map.of("tenantId", tenantId), Long.class);
        List<ResourceDetail<EntitlementRecord>> items = jdbc.query("""
                SELECT id,application_id,enabled,source,source_id,valid_from,valid_until,version
                  FROM iam_tenant_app_entitlement WHERE tenant_id=:tenantId
                 ORDER BY application_id LIMIT :limit OFFSET :offset
                """, Map.of("tenantId", tenantId, "limit", pageSize, "offset", IamPages.offset(page, pageSize)),
                (row, index) -> IamDetails.of(entitlement(row), row.getString("version")));
        return IamPages.details(items, total == null ? 0 : total, page, pageSize);
    }

    private List<ValidationIssue> validateReplace(long tenantId, EntitlementReplaceInput input, boolean requireVersion) {
        List<ValidationIssue> errors = new ArrayList<>();
        if (requireVersion) {
            try {
                IamIds.requireExpected(input.expectedVersion(), collectionVersion(tenantId));
            } catch (BizException exception) {
                errors.add(new ValidationIssue("expectedVersion", IamReasonCode.REVISION_CONFLICT, "开通配置已变化，请重新预览后提交"));
            }
        }
        Set<String> requested = new LinkedHashSet<>();
        Set<Long> baseline = new LinkedHashSet<>(jdbc.queryForList("""
                SELECT id FROM iam_application
                 WHERE domain=:domain AND enabled=TRUE AND baseline=TRUE
                """, Map.of("domain", AuthorizationDomain.TENANT.name()), Long.class));
        Set<Long> enabledBaseline = new LinkedHashSet<>();
        int index = 0;
        for (EntitlementDraft draft : input.entitlements()) {
            String path = "entitlements[" + index++ + "]";
            if (!requested.add(draft.applicationId())) {
                errors.add(new ValidationIssue(path, IamReasonCode.INVALID_ARGUMENT, "同一应用不能出现多条开通"));
                continue;
            }
            long applicationId;
            try {
                applicationId = IamIds.require(draft.applicationId());
            } catch (BizException exception) {
                errors.add(new ValidationIssue(path + ".applicationId", IamReasonCode.INVALID_ARGUMENT, "应用 ID 不合法"));
                continue;
            }
            List<Map<String, Object>> applications = jdbc.queryForList("""
                    SELECT domain,enabled,baseline FROM iam_application WHERE id=:id
                    """, Map.of("id", applicationId));
            if (applications.size() != 1) {
                errors.add(new ValidationIssue(path + ".applicationId", IamReasonCode.OBJECT_NOT_FOUND, "应用不存在"));
                continue;
            }
            Map<String, Object> application = applications.getFirst();
            if (!AuthorizationDomain.TENANT.name().equals(String.valueOf(application.get("domain")))) {
                errors.add(new ValidationIssue(path + ".applicationId", IamReasonCode.APPLICATION_UNAVAILABLE, "只能开通租户域应用"));
            }
            if (Boolean.TRUE.equals(asBoolean(application.get("baseline")))
                    && draft.status() == ConfigurationStatus.ENABLED) {
                enabledBaseline.add(applicationId);
            }
        }
        if (!enabledBaseline.containsAll(baseline)) {
            errors.add(new ValidationIssue("entitlements", IamReasonCode.APPLICATION_UNAVAILABLE, "基础治理入口不可关闭"));
        }
        return errors;
    }

    private ResourceDetail<AudienceDraft> loadAudience(long tenantId, long applicationId) {
        if (!entitled(tenantId, applicationId)) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        List<AudienceRow> rows = jdbc.query("""
                SELECT audience_kind,version FROM iam_app_audience
                 WHERE tenant_id=:tenantId AND application_id=:applicationId
                """, Map.of("tenantId", tenantId, "applicationId", applicationId),
                (row, index) -> new AudienceRow(AudienceKind.valueOf(row.getString("audience_kind")),
                        row.getString("version")));
        if (rows.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        AudienceRow row = rows.getFirst();
        if (row.kind() == AudienceKind.ALL) {
            return IamDetails.of(new AudienceDraft(AudienceKind.ALL, null, List.of()), row.version());
        }
        List<String> members = jdbc.queryForList("""
                SELECT member_id FROM iam_audience_member
                 WHERE tenant_id=:tenantId AND application_id=:applicationId ORDER BY member_id
                """, Map.of("tenantId", tenantId, "applicationId", applicationId), Long.class)
                .stream().map(IamIds::text).toList();
        List<DepartmentSelection> departments = jdbc.query("""
                SELECT department_id,include_descendants FROM iam_audience_department
                 WHERE tenant_id=:tenantId AND application_id=:applicationId ORDER BY department_id
                """, Map.of("tenantId", tenantId, "applicationId", applicationId),
                (item, index) -> new DepartmentSelection(item.getString("department_id"),
                        item.getBoolean("include_descendants")));
        List<String> groups = jdbc.queryForList("""
                SELECT group_id FROM iam_audience_group
                 WHERE tenant_id=:tenantId AND application_id=:applicationId ORDER BY group_id
                """, Map.of("tenantId", tenantId, "applicationId", applicationId), Long.class)
                .stream().map(IamIds::text).toList();
        return IamDetails.of(new AudienceDraft(AudienceKind.SELECTED, new Selection(members, departments), groups),
                row.version());
    }

    private void writeSelection(long tenantId, long applicationId, AudienceDraft audience) {
        Selection selection = audience.selection() == null ? new Selection(List.of(), List.of()) : audience.selection();
        selection.requireCompatibleDomain(AuthorizationDomain.TENANT);
        for (String memberId : selection.members()) {
            long id = IamIds.require(memberId);
            Long count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM iam_tenant_member
                     WHERE tenant_id=:tenantId AND id=:id AND status<>'REMOVED'
                    """, Map.of("tenantId", tenantId, "id", id), Long.class);
            if (count == null || count != 1) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            jdbc.update("""
                    INSERT INTO iam_audience_member(tenant_id,application_id,member_id)
                    VALUES (:tenantId,:applicationId,:memberId)
                    """, Map.of("tenantId", tenantId, "applicationId", applicationId, "memberId", id));
        }
        for (DepartmentSelection department : selection.departments()) {
            long id = IamIds.require(department.id());
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_department WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", tenantId, "id", id), Long.class);
            if (count == null || count != 1) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            jdbc.update("""
                    INSERT INTO iam_audience_department(tenant_id,application_id,department_id,include_descendants)
                    VALUES (:tenantId,:applicationId,:departmentId,:includeDescendants)
                    """, Map.of("tenantId", tenantId, "applicationId", applicationId, "departmentId", id,
                    "includeDescendants", department.includeDescendants()));
        }
        for (String groupId : audience.groupIds()) {
            long id = IamIds.require(groupId);
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM iam_tenant_group WHERE tenant_id=:tenantId AND id=:id",
                    Map.of("tenantId", tenantId, "id", id), Long.class);
            if (count == null || count != 1) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            jdbc.update("""
                    INSERT INTO iam_audience_group(tenant_id,application_id,group_id)
                    VALUES (:tenantId,:applicationId,:groupId)
                    """, Map.of("tenantId", tenantId, "applicationId", applicationId, "groupId", id));
        }
    }

    private boolean entitled(long tenantId, long applicationId) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_tenant_app_entitlement
                 WHERE tenant_id=:tenantId AND application_id=:applicationId AND enabled=TRUE
                   AND (valid_from IS NULL OR valid_from<=CURRENT_TIMESTAMP)
                   AND (valid_until IS NULL OR valid_until>CURRENT_TIMESTAMP)
                """, Map.of("tenantId", tenantId, "applicationId", applicationId), Long.class);
        return count != null && count == 1;
    }

    private void requireTenant(long tenantId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_tenant WHERE id=:id AND deleted_at IS NULL", Map.of("id", tenantId), Long.class);
        if (count == null || count != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private String collectionVersion(long tenantId) {
        List<String> parts = jdbc.query("""
                SELECT application_id,version,enabled FROM iam_tenant_app_entitlement
                 WHERE tenant_id=:tenantId ORDER BY application_id
                """, Map.of("tenantId", tenantId),
                (row, index) -> row.getLong("application_id") + ":" + row.getLong("version") + ":"
                        + (row.getBoolean("enabled") ? "1" : "0"));
        return parts.isEmpty() ? "0" : parts.stream().collect(Collectors.joining("|"));
    }

    private static EntitlementRecord entitlement(java.sql.ResultSet row) throws java.sql.SQLException {
        Timestamp from = row.getTimestamp("valid_from");
        Timestamp until = row.getTimestamp("valid_until");
        Object sourceId = row.getObject("source_id");
        return new EntitlementRecord(row.getString("id"), row.getString("application_id"),
                row.getBoolean("enabled") ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED,
                EntitlementSource.valueOf(row.getString("source")),
                sourceId == null ? null : sourceId.toString(),
                from == null ? null : from.toInstant(), until == null ? null : until.toInstant());
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    private static String nextVersion(String current) {
        return Long.toString(Long.parseLong(current) + 1);
    }

    private record AudienceRow(AudienceKind kind, String version) {
    }
}
