package com.ingot.cloud.iam.catalog;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.CatalogRepository;
import com.ingot.cloud.iam.persistence.EntitlementRepository;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.persistence.entity.IamAppAudienceEntity;
import com.ingot.cloud.iam.persistence.entity.IamAudienceDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantAppEntitlementEntity;
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
public class EntitlementService {
    private static final String ENTITLEMENT = "entitlement";
    private static final String AUDIENCE = "audience";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final CatalogRepository catalog;
    private final EntitlementRepository entitlements;
    private final TenantRepository tenants;
    private final EntitlementResolver resolver;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、目录与开通持久化以及事务。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。
     * 测试夹具应注入 {@link CatalogRepository} 与 {@link EntitlementRepository}，不要再传入 DataSource。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 开通与人群变更后的授权失效
     * @param catalog 操作分页、基础应用与应用目录
     * @param entitlements 开通与人群读写
     * @param tenants 组织套餐回写
     * @param resolver 套餐与自选并集
     * @param transactionManager 同一数据源事务
     */
    public EntitlementService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                  CatalogRepository catalog, EntitlementRepository entitlements,
                                  TenantRepository tenants, EntitlementResolver resolver,
                                  PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.catalog = catalog;
        this.entitlements = entitlements;
        this.tenants = tenants;
        this.resolver = resolver;
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
        if (!entitlements.entitled(tenantId, appId)) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        IamPages.require(page, pageSize);
        Page<IamActionEntity> rows = catalog.pageActions(appId, page, pageSize, null, null, List.of());
        List<ResourceDetail<ActionRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(action(row), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
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
        List<ResolvedEntitlement> resolved;
        try {
            resolved = resolver.resolve(input.planId(), input.entitlements());
        } catch (BizException exception) {
            IamReasonCode code = IamReasonCode.INVALID_ARGUMENT.getCode().equals(exception.getCode())
                    ? IamReasonCode.INVALID_ARGUMENT : IamReasonCode.APPLICATION_UNAVAILABLE;
            ValidationIssue error = new ValidationIssue("entitlements", code, "开通应用不可用或套餐无效");
            return new Preview<>(entitlements.collectionVersion(id), false, List.of(error), List.of(),
                    new ImpactSummary(null, null, null, false), null);
        }
        List<ValidationIssue> errors = validateResolved(id, input, resolved, false);
        EntitlementPreviewResult result = new EntitlementPreviewResult(
                resolved.stream().map(ResolvedEntitlement::toPreview).toList(),
                new ImpactSummary(null, null, null, false));
        return new Preview<>(entitlements.collectionVersion(id), errors.isEmpty(), errors, List.of(),
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
            IamIds.requireExpected(input.expectedVersion(), entitlements.collectionVersion(id));
            List<ResolvedEntitlement> resolved = resolver.resolve(input.planId(), input.entitlements());
            List<ValidationIssue> errors = validateResolved(id, input, resolved, true);
            if (!errors.isEmpty()) {
                throw new BizException(errors.getFirst().code());
            }
            entitlements.deleteAllForTenant(id);
            LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
            for (ResolvedEntitlement item : resolved) {
                long applicationId = IamIds.require(item.application().id());
                IamTenantAppEntitlementEntity entitlement = new IamTenantAppEntitlementEntity();
                entitlement.setId(BigInteger.valueOf(access.nextId()));
                entitlement.setTenantId(BigInteger.valueOf(id));
                entitlement.setApplicationId(BigInteger.valueOf(applicationId));
                entitlement.setEnabled(item.status() == ConfigurationStatus.ENABLED);
                entitlement.setSource(item.source());
                entitlement.setSourceId(item.sourceId() == null ? null : BigInteger.valueOf(Long.parseLong(item.sourceId())));
                entitlement.setValidFrom(item.validFrom() == null ? now : utc(item.validFrom()));
                entitlement.setValidUntil(item.validUntil() == null ? null : utc(item.validUntil()));
                entitlements.insertEntitlement(entitlement);
                IamAppAudienceEntity audience = new IamAppAudienceEntity();
                audience.setTenantId(BigInteger.valueOf(id));
                audience.setApplicationId(BigInteger.valueOf(applicationId));
                audience.setAudienceKind(AudienceKind.ALL);
                entitlements.insertAudience(audience);
            }
            tenants.updatePlanId(id, blank(input.planId()) ? null : Long.parseLong(input.planId()));
            audits.write(actor.context(), access.nextId(), ENTITLEMENT, tenantId, AuditChangeType.UPDATE,
                    Map.of(), Map.of(AuditField.ENTITLEMENT, Integer.toString(resolved.size())),
                    Map.of(ENTITLEMENT, entitlements.collectionVersion(id)));
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
            if (!entitlements.entitled(tenantId, appId)) {
                throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
            }
            ResourceDetail<AudienceDraft> current = loadAudience(tenantId, appId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            entitlements.deleteAudienceSelections(tenantId, appId);
            entitlements.updateAudience(tenantId, appId, audience.kind(), new BigInteger(current.version()));
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
        Page<IamTenantAppEntitlementEntity> rows = entitlements.pageEntitlements(tenantId, page, pageSize);
        Map<BigInteger, String> names = catalog.applicationNames(rows.getRecords().stream()
                .map(IamTenantAppEntitlementEntity::getApplicationId).toList());
        List<ResourceDetail<EntitlementRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(entitlement(row, names), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    private List<ValidationIssue> validateResolved(long tenantId, EntitlementReplaceInput input,
                                                  List<ResolvedEntitlement> resolved, boolean requireVersion) {
        List<ValidationIssue> errors = new ArrayList<>();
        if (requireVersion) {
            try {
                IamIds.requireExpected(input.expectedVersion(), entitlements.collectionVersion(tenantId));
            } catch (BizException exception) {
                errors.add(new ValidationIssue("expectedVersion", IamReasonCode.REVISION_CONFLICT, "开通配置已变化，请重新预览后提交"));
            }
        } else if (!input.expectedVersion().equals(entitlements.collectionVersion(tenantId))) {
            errors.add(new ValidationIssue("expectedVersion", IamReasonCode.REVISION_CONFLICT, "开通配置已变化，请重新预览后提交"));
        }
        Set<Long> baseline = new LinkedHashSet<>();
        for (BigInteger applicationId : catalog.enabledBaselineIds(AuthorizationDomain.TENANT)) {
            baseline.add(applicationId.longValue());
        }
        Set<Long> enabledBaseline = new LinkedHashSet<>();
        for (ResolvedEntitlement item : resolved) {
            if (item.status() == ConfigurationStatus.ENABLED) {
                enabledBaseline.add(Long.parseLong(item.application().id()));
            }
        }
        if (!enabledBaseline.containsAll(baseline)) {
            errors.add(new ValidationIssue("entitlements", IamReasonCode.APPLICATION_UNAVAILABLE, "基础治理入口不可关闭"));
        }
        return errors;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ResourceDetail<AudienceDraft> loadAudience(long tenantId, long applicationId) {
        if (!entitlements.entitled(tenantId, applicationId)) {
            throw new BizException(IamReasonCode.APPLICATION_UNAVAILABLE);
        }
        IamAppAudienceEntity row = entitlements.findAudience(tenantId, applicationId);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        if (row.getAudienceKind() == AudienceKind.ALL) {
            return IamDetails.of(new AudienceDraft(AudienceKind.ALL, null, List.of()), version(row.getVersion()));
        }
        List<String> members = entitlements.audienceMemberIds(tenantId, applicationId).stream()
                .map(EntitlementService::text).toList();
        List<DepartmentSelection> departments = entitlements.audienceDepartments(tenantId, applicationId).stream()
                .map(EntitlementService::department).toList();
        List<String> groups = entitlements.audienceGroupIds(tenantId, applicationId).stream()
                .map(EntitlementService::text).toList();
        return IamDetails.of(new AudienceDraft(AudienceKind.SELECTED, new Selection(members, departments), groups),
                version(row.getVersion()));
    }

    private void writeSelection(long tenantId, long applicationId, AudienceDraft audience) {
        Selection selection = audience.selection() == null ? new Selection(List.of(), List.of()) : audience.selection();
        selection.requireCompatibleDomain(AuthorizationDomain.TENANT);
        for (String memberId : selection.members()) {
            long id = IamIds.require(memberId);
            if (!entitlements.existsActiveMember(tenantId, id)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            entitlements.insertAudienceMember(tenantId, applicationId, id);
        }
        for (DepartmentSelection department : selection.departments()) {
            long id = IamIds.require(department.id());
            if (!entitlements.existsDepartment(tenantId, id)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            entitlements.insertAudienceDepartment(tenantId, applicationId, id, department.includeDescendants());
        }
        for (String groupId : audience.groupIds()) {
            long id = IamIds.require(groupId);
            if (!entitlements.existsGroup(tenantId, id)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            entitlements.insertAudienceGroup(tenantId, applicationId, id);
        }
    }

    private void requireTenant(long tenantId) {
        if (!entitlements.existsActiveTenant(tenantId)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private static EntitlementRecord entitlement(IamTenantAppEntitlementEntity row, Map<BigInteger, String> names) {
        BigInteger applicationId = row.getApplicationId();
        return new EntitlementRecord(text(row.getId()), text(applicationId),
                applicationId == null ? null : names.get(applicationId),
                statusOf(row.getEnabled()), row.getSource(),
                row.getSourceId() == null ? null : row.getSourceId().toString(),
                instant(row.getValidFrom()), instant(row.getValidUntil()));
    }

    private static ActionRecord action(IamActionEntity row) {
        return new ActionRecord(text(row.getId()), text(row.getApplicationId()), text(row.getResourceId()),
                row.getCode(), row.getName(), statusOf(row.getEnabled()));
    }

    private static DepartmentSelection department(IamAudienceDepartmentEntity row) {
        return new DepartmentSelection(text(row.getDepartmentId()), Boolean.TRUE.equals(row.getIncludeDescendants()));
    }

    private static ConfigurationStatus statusOf(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED;
    }

    private static String nextVersion(String current) {
        return Long.toString(Long.parseLong(current) + 1);
    }

    private static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    private static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    private static LocalDateTime utc(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
