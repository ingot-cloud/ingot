package com.ingot.cloud.iam.tenant;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.OwnerTransferInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.TenantRecord;
import com.ingot.framework.commons.model.iam.TenantSettingsInput;
import com.ingot.framework.commons.model.iam.TenantUpdateInput;
import com.ingot.framework.commons.model.iam.VersionInput;
import com.ingot.cloud.iam.persistence.TenantRepository;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护平台可见组织实体与租户可编辑设置，所有者转交走独立命令。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class TenantQueryService {
    private static final String TENANT = "tenant";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final TenantRepository tenants;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计与组织表。
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param tenants 组织持久化
     * @param transactionManager 同一数据源事务
     */
    public TenantQueryService(IamAccess access, IamAuditWriter audits, TenantRepository tenants,
                                  PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.tenants = tenants;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出平台组织实体。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 组织页
     */
    public PageResponse<ResourceDetail<TenantRecord>> list(int page, int pageSize) {
        access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_TENANT_READ);
        IamPages.require(page, pageSize);
        var result = tenants.page(page, pageSize);
        List<ResourceDetail<TenantRecord>> items = result.getRecords().stream()
                .map(row -> IamDetails.of(record(row), row.getVersion().toString())).toList();
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
            tenants.update(tenantId, input.name(), input.avatar(),
                    input.status() == ConfigurationStatus.ENABLED, new BigInteger(current.version()));
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
        return load(IamIds.require(actor.context().tenantId()));
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
            tenants.update(tenantId, input.name(), input.avatar(), null, new BigInteger(current.version()));
            audits.write(actor.context(), access.nextId(), TENANT, IamIds.text(tenantId), AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(AuditField.NAME, input.name()),
                    Map.of(TENANT, Long.toString(Long.parseLong(current.version()) + 1)));
            return load(tenantId);
        });
    }

    /**
     * 原子转交所有者，不能移除最后所有者。
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
            if (!tenants.hasActiveMember(tenantId, newOwner)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            tenants.transferOwner(tenantId, newOwner, new BigInteger(current.version()));
            String version = Long.toString(Long.parseLong(current.version()) + 1);
            audits.write(actor.context(), access.nextId(), TENANT, IamIds.text(tenantId), AuditChangeType.UPDATE,
                    Map.of(AuditField.OWNER_MEMBER, current.record().ownerMemberId()),
                    Map.of(AuditField.OWNER_MEMBER, input.newOwnerMemberId()), Map.of(TENANT, version));
            return new CreatedResource(IamIds.text(tenantId), version);
        });
    }

    private ResourceDetail<TenantRecord> load(long id) {
        return detail(tenants.findActive(id));
    }

    private ResourceDetail<TenantRecord> lock(long id) {
        return detail(tenants.lockActive(id));
    }

    private ResourceDetail<TenantRecord> detail(IamTenantEntity row) {
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(record(row), row.getVersion().toString());
    }

    private static TenantRecord record(IamTenantEntity row) {
        return new TenantRecord(row.getId().toString(), row.getName(), row.getAvatar(),
                row.getOwnerMemberId() == null ? null : row.getOwnerMemberId().toString(),
                Boolean.TRUE.equals(row.getEnabled()) ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED);
    }
}
