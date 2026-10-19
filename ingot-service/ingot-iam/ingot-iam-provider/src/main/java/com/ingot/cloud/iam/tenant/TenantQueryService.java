package com.ingot.cloud.iam.tenant;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamFilters;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AssignmentSource;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.OwnerTransferInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.TenantRecord;
import com.ingot.framework.commons.model.iam.TenantSettingsInput;
import com.ingot.framework.commons.model.iam.TenantUpdateInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护平台可见组织实体与租户可编辑设置，所有者转交走独立命令并同步治理授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TenantQueryService {
    private static final String TENANT = "tenant";
    private static final String ASSIGNMENT = "assignment";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final TenantRepository tenants;
    private final AssignmentRepository assignments;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效、组织与分配表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param tenants 组织持久化
     * @param assignments 角色分配
     * @param transactionManager 同一数据源事务
     */
    public TenantQueryService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                              TenantRepository tenants, AssignmentRepository assignments,
                              PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.tenants = tenants;
        this.assignments = assignments;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出平台组织实体。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 组织名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 组织页
     */
    public PageResponse<ResourceDetail<TenantRecord>> list(int page, int pageSize, String name, String status) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_TENANT_READ);
        IamPages.require(page, pageSize);
        var result = tenants.page(page, pageSize, name, IamFilters.enabledOf(status));
        List<IamTenantEntity> rows = result.getRecords();
        Map<BigInteger, TenantRepository.OwnerContact> owners = tenants.ownerContacts(rows.stream()
                .map(IamTenantEntity::getOwnerMemberId).toList());
        List<ResourceDetail<TenantRecord>> items = rows.stream()
                .map(row -> IamDetails.of(record(row, owners), row.getVersion().toString())).toList();
        return IamPages.details(items, result.getTotal(), page, pageSize);
    }

    /**
     * 读取平台组织实体。
     *
     * @param id 组织 ID
     * @return 组织详情
     */
    public ResourceDetail<TenantRecord> get(String id) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_TENANT_READ);
        return load(IamIds.require(id));
    }

    /**
     * 更新平台可见组织实体，不修改租户业务成员。
     *
     * @param id 组织 ID
     * @param input 名称、头像与启停
     * @return 更新后详情
     */
    public ResourceDetail<TenantRecord> patch(String id, TenantUpdateInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_TENANT_UPDATE);
        long tenantId = IamIds.require(id);
        return transaction.execute(status -> {
            ResourceDetail<TenantRecord> current = lock(tenantId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            requireApplied(tenants.update(tenantId, input.name(), input.avatar(),
                    input.status() == ConfigurationStatus.ENABLED, new BigInteger(current.version())));
            audits.write(actor.context(), access.nextId(), TENANT, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(AuditField.NAME, input.name()),
                    Map.of(TENANT, Long.toString(Long.parseLong(current.version()) + 1)));
            return load(tenantId);
        });
    }

    /**
     * 读取当前组织设置。
     *
     * @return 组织详情
     */
    public ResourceDetail<TenantRecord> settings() {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_SETTINGS_READ);
        var result = load(IamIds.require(actor.context().tenantId()));
        boolean owner = actor.context().memberId().equals(result.record().ownerMemberId());
        var capabilities = access.capabilities(actor, List.of(IamAction.TENANT_SETTINGS_UPDATE, IamAction.TENANT_SETTINGS_OWNER_TRANSFER));
        boolean transfer = owner && capabilities.allows(IamAction.TENANT_SETTINGS_OWNER_TRANSFER, true);
        return IamDetails.of(result.record(), Map.of(
            IamAction.TENANT_SETTINGS_UPDATE.getCode(), new com.ingot.framework.commons.model.iam.ObjectCapability(capabilities.allows(IamAction.TENANT_SETTINGS_UPDATE,false), null, null),
            IamAction.TENANT_SETTINGS_OWNER_TRANSFER.getCode(), new com.ingot.framework.commons.model.iam.ObjectCapability(transfer, transfer ? null : IamReasonCode.ACTION_DENIED, transfer ? null : "仅当前组织拥有者可以转交")), result.version());
    }

    /**
     * 更新当前组织可编辑设置。
     *
     * @param input 名称与头像
     * @return 更新后详情
     */
    public ResourceDetail<TenantRecord> updateSettings(TenantSettingsInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_SETTINGS_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> {
            ResourceDetail<TenantRecord> current = lock(tenantId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            requireApplied(tenants.update(tenantId, input.name(), input.avatar(), null,
                    new BigInteger(current.version())));
            audits.write(actor.context(), access.nextId(), TENANT, IamIds.text(tenantId), AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(AuditField.NAME, input.name()),
                    Map.of(TENANT, Long.toString(Long.parseLong(current.version()) + 1)));
            return load(tenantId);
        });
    }

    /**
     * 原子转交当前所有者身份，独立角色分配保留，提交后动态权限按新身份求值。
     *
     * @param input 新所有者
     * @return 提交后版本
     */
    public CreatedResource transferOwner(OwnerTransferInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_SETTINGS_OWNER_TRANSFER);
        long tenantId = IamIds.require(actor.context().tenantId());
        long newOwner = IamIds.require(input.newOwnerMemberId());
        return transaction.execute(status -> {
            ResourceDetail<TenantRecord> current = lock(tenantId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            long oldOwner = IamIds.require(current.record().ownerMemberId());
            if (!actor.context().memberId().equals(current.record().ownerMemberId()))
                throw new BizException(IamReasonCode.ACTION_DENIED);
            if (oldOwner == newOwner) {
                return new CreatedResource(IamIds.text(tenantId), current.version());
            }
            lockMembers(tenantId, oldOwner, newOwner);
            requireApplied(tenants.transferOwner(tenantId, newOwner, new BigInteger(current.version())));
            String version = Long.toString(Long.parseLong(current.version()) + 1);
            audits.write(actor.context(), access.nextId(), TENANT, IamIds.text(tenantId), AuditChangeType.OWNER_TRANSFER,
                    Map.of(AuditField.OWNER_MEMBER, current.record().ownerMemberId()),
                    Map.of(AuditField.OWNER_MEMBER, input.newOwnerMemberId()), Map.of(TENANT, version));
            changes.markAll();
            return new CreatedResource(IamIds.text(tenantId), version);
        });
    }

    private void lockMembers(long tenantId, long oldOwner, long newOwner) {
        long first = Math.min(oldOwner, newOwner);
        long second = Math.max(oldOwner, newOwner);
        IamTenantMemberEntity firstRow = requireMember(tenantId, first);
        IamTenantMemberEntity secondRow = first == second ? firstRow : requireMember(tenantId, second);
        IamTenantMemberEntity oldRow = oldOwner == first ? firstRow : secondRow;
        IamTenantMemberEntity newRow = newOwner == first ? firstRow : secondRow;
        if (oldRow.getStatus() != MemberStatus.ACTIVE) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        if (newRow.getStatus() != MemberStatus.ACTIVE) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private IamTenantMemberEntity requireMember(long tenantId, long memberId) {
        IamTenantMemberEntity row = tenants.lockMember(tenantId, memberId);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return row;
    }

    private ResourceDetail<TenantRecord> load(long id) {
        return detail(tenants.findActive(id), true);
    }

    private ResourceDetail<TenantRecord> lock(long id) {
        return detail(tenants.lockActive(id), false);
    }

    private ResourceDetail<TenantRecord> detail(IamTenantEntity row, boolean includeOwnerName) {
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        Map<BigInteger, TenantRepository.OwnerContact> owners = includeOwnerName
                ? tenants.ownerContacts(row.getOwnerMemberId() == null ? List.of() : List.of(row.getOwnerMemberId()))
                : Map.of();
        return IamDetails.of(record(row, owners), row.getVersion().toString());
    }

    private static TenantRecord record(IamTenantEntity row, Map<BigInteger, TenantRepository.OwnerContact> owners) {
        BigInteger ownerId = row.getOwnerMemberId();
        TenantRepository.OwnerContact owner = ownerId == null ? null : owners.get(ownerId);
        return new TenantRecord(row.getId().toString(), row.getName(), row.getAvatar(),
                ownerId == null ? null : ownerId.toString(),
                owner == null ? null : owner.displayName(),
                owner == null ? null : owner.phone(),
                owner == null ? null : owner.email(),
                Boolean.TRUE.equals(row.getEnabled()) ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED,
                instant(row.getCreatedAt()),
                row.getPlanId() == null ? null : row.getPlanId().toString());
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static void requireApplied(int rows) {
        if (rows != 1) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
    }

    private static String nextVersion(BigInteger version) {
        return version == null ? "1" : version.add(BigInteger.ONE).toString();
    }
}
