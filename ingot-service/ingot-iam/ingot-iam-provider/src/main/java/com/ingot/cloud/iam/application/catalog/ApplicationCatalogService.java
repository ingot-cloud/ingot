package com.ingot.cloud.iam.application.catalog;

import com.ingot.cloud.iam.domain.catalog.CatalogInUse;
import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.application.catalog.model.ApplicationDraft;
import com.ingot.cloud.iam.application.catalog.model.ApplicationPurgeInput;
import com.ingot.cloud.iam.application.catalog.model.ApplicationRecord;
import com.ingot.cloud.iam.domain.catalog.ApplicationSummary;
import com.ingot.cloud.iam.application.catalog.model.ApplicationUpdateInput;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.application.shared.model.ConfigurationStatusInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>维护对应目录对象并保留原授权、审计与事务顺序。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ApplicationCatalogService {
    private final CatalogOperations operationsContext;

    /**
     * 分页列出应用目录。
     * @param page 从 1 开始
     * @param pageSize 页大小
     * @param domain {@link AuthorizationDomain} 稳定字面量，必填
     * @param name 应用名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用详情页
     */
    public PageResponse<ResourceDetail<ApplicationRecord>> listApplications(int page, int pageSize, String domain,
            String name, String status, Boolean baseline) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_READ);
        IamPages.require(page, pageSize);
        Page<IamApplicationEntity> rows = operationsContext.catalog().pageApplications(page, pageSize, IamFilters.requireDomain(domain),
                name, IamFilters.enabledOf(status), baseline);
        List<ResourceDetail<ApplicationRecord>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(application(row), CatalogOperations.version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 分页列出开通选择器所需的应用摘要。
     * @param page 从 1 开始的页码
     * @param pageSize 每页条数
     * @param domain 管理域，必填 PLATFORM 或 TENANT
     * @param name 应用名称包含匹配，空白表示不限制
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @param baseline 是否组织默认开通，空表示不限制
     * @return 应用摘要页
     */
    public PageResponse<ResourceDetail<ApplicationSummary>> listApplicationSummaries(int page, int pageSize,
            String domain, String name, String status, Boolean baseline) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_READ);
        IamPages.require(page, pageSize);
        Page<IamApplicationEntity> rows = operationsContext.catalog().pageApplications(page, pageSize, IamFilters.requireDomain(domain),
                name, IamFilters.enabledOf(status), baseline);
        List<ResourceDetail<ApplicationSummary>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(applicationSummary(row), CatalogOperations.version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取单个应用。
     * @param id 应用 ID
     * @return 应用详情
     */
    public ResourceDetail<ApplicationRecord> getApplication(String id) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_READ);
        return loadApplication(IamIds.require(id));
    }

    /**
     * 创建应用目录项，不自动为任何租户开通。
     * @param input 创建草稿
     * @return 新应用 ID 与版本
     */
    public CreatedResource createApplication(ApplicationDraft input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_CREATE);
        if (input.domain() == AuthorizationDomain.PLATFORM && input.baseline()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return operationsContext.transaction().execute(status -> {
            long id = operationsContext.access().nextId();
            IamApplicationEntity entity = new IamApplicationEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setCode(input.code());
            entity.setDomain(input.domain());
            entity.setName(input.name());
            entity.setDescription(CatalogOperations.nullable(input.description()));
            operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_CREATE);
            entity.setIcon(operationsContext.images().bind(actor.context(), ImageUploadPurpose.APPLICATION_CREATE, null, CatalogOperations.nullable(input.icon()), null));
            entity.setSortOrder(input.sortOrder());
            entity.setBaseline(input.baseline());
            entity.setEnabled(true);
            try {
                operationsContext.catalog().insertApplication(entity);
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.APPLICATION, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(CatalogOperations.APPLICATION, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新展示信息与基础开通标记，不能改写命名空间或管理域。
     * @param id 应用 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ApplicationRecord> updateApplication(String id, ApplicationUpdateInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_UPDATE);
        long applicationId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            IamApplicationEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockApplication(applicationId));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            if (current.getDomain() == AuthorizationDomain.PLATFORM && input.baseline()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_UPDATE);
            operationsContext.catalog().updateApplication(applicationId, input.name(), CatalogOperations.nullable(input.description()),
                    operationsContext.images().bind(actor.context(), ImageUploadPurpose.APPLICATION_UPDATE, IamIds.text(applicationId),
                            CatalogOperations.nullable(input.icon()), current.getIcon()), input.sortOrder(), input.baseline(),
                    current.getVersion());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.APPLICATION, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, input.name()),
                    Map.of(CatalogOperations.APPLICATION, next));
            return loadApplication(applicationId);
        });
    }

    /**
     * 全局启停应用，停用后该应用全部操作失败关闭。
     * @param id 应用 ID
     * @param input 目标状态与版本
     * @return 提交后版本
     */
    public CreatedResource changeApplicationStatus(String id, ConfigurationStatusInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_STATUS);
        long applicationId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            IamApplicationEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockApplication(applicationId));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            boolean enabled = input.status() == ConfigurationStatus.ENABLED;
            if (Boolean.TRUE.equals(current.getEnabled()) == enabled) {
                return new CreatedResource(id, CatalogOperations.version(current.getVersion()));
            }
            operationsContext.catalog().updateApplicationEnabled(applicationId, enabled, current.getVersion());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.APPLICATION, id,
                    enabled ? AuditChangeType.ENABLE : AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, CatalogOperations.statusOf(current.getEnabled()).name()),
                    Map.of(AuditField.STATUS, input.status().name()), Map.of(CatalogOperations.APPLICATION, next));
            operationsContext.changes().markAll();
            return new CreatedResource(id, next);
        });
    }

    /**
     * 删除未被资源、菜单、开通或套餐引用的应用；被拒时说明具体引用。
     * @param id 应用 ID
     * @return 删除前版本
     */
    public CreatedResource deleteApplication(String id) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_DELETE);
        long applicationId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            IamApplicationEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockApplication(applicationId));
            CatalogInUse.requireApplicationUnused(operationsContext.catalog().countResources(applicationId),
                    operationsContext.catalog().countMenus(applicationId), operationsContext.catalog().countEntitlements(applicationId),
                    operationsContext.catalog().countPlanApplications(applicationId));
            operationsContext.catalog().deleteApplication(applicationId);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.APPLICATION, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(CatalogOperations.APPLICATION, CatalogOperations.version(current.getVersion())));
            return new CreatedResource(id, CatalogOperations.version(current.getVersion()));
        });
    }

    /**
     * 确认当前账号后，同一事务清除该应用全部关联并删除应用行。
     * @param id 应用 ID
     * @param input 版本与内嵌确认
     * @return 删除前版本
     */
    public CreatedResource purgeApplication(String id, ApplicationPurgeInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_PURGE);
        operationsContext.confirmations().require(actor, input.confirmation());
        long applicationId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            IamApplicationEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockApplication(applicationId));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            if (isGovernanceApplication(current.getCode())) {
                throw new BizException(IamReasonCode.ACTION_DENIED.getCode(), CatalogOperations.GOVERNANCE_PURGE_DENIED);
            }
            operationsContext.catalog().purgeApplication(applicationId);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.APPLICATION, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(CatalogOperations.APPLICATION, CatalogOperations.version(current.getVersion())));
            operationsContext.changes().markAll();
            return new CreatedResource(id, CatalogOperations.version(current.getVersion()));
        });
    }

    private ResourceDetail<ApplicationRecord> loadApplication(long id) {
        IamApplicationEntity row = operationsContext.catalog().findApplication(id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return ResourceDetailFactory.of(application(row), CatalogOperations.version(row.getVersion()));
    }

    private static boolean isGovernanceApplication(String code) {
        return CatalogOperations.GOVERNANCE_PLATFORM_CODE.equals(code) || CatalogOperations.GOVERNANCE_TENANT_CODE.equals(code);
    }

    private static ApplicationRecord application(IamApplicationEntity row) {
        return new ApplicationRecord(CatalogOperations.text(row.getId()), row.getCode(), row.getDomain(), row.getName(),
                row.getDescription(), row.getIcon(), row.getSortOrder() == null ? 0 : row.getSortOrder(),
                Boolean.TRUE.equals(row.getBaseline()), CatalogOperations.statusOf(row.getEnabled()));
    }

    private static ApplicationSummary applicationSummary(IamApplicationEntity row) {
        return new ApplicationSummary(CatalogOperations.text(row.getId()), row.getCode(), row.getName(), row.getIcon(),
                row.getSortOrder() == null ? 0 : row.getSortOrder());
    }
}
