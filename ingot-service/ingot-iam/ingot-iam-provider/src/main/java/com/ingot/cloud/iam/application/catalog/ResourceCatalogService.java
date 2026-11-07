package com.ingot.cloud.iam.application.catalog;

import com.ingot.cloud.iam.domain.catalog.CatalogInUse;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.catalog.model.ResourceDraft;
import com.ingot.cloud.iam.application.catalog.model.ResourceRecord;
import com.ingot.cloud.iam.application.catalog.model.ResourceUpdateInput;
import com.ingot.framework.commons.model.iam.ScopeKind;
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
public class ResourceCatalogService {
    private final CatalogOperations operationsContext;

    /**
     * 列出应用内资源。
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param name 资源名称包含匹配，空白表示不限制
     * @param code 资源编码包含匹配，空白表示不限制
     * @return 资源页
     */
    public PageResponse<ResourceDetail<ResourceRecord>> listResources(String applicationId, int page, int pageSize,
            String name, String code) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_READ);
        long id = IamIds.require(applicationId);
        operationsContext.requireApplication(id);
        IamApplicationEntity application = operationsContext.catalog().findApplication(id);
        IamPages.require(page, pageSize);
        Page<IamResourceEntity> rows = operationsContext.catalog().pageResources(id, page, pageSize, name, code);
        List<ResourceDetail<ResourceRecord>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(resource(application, row), CatalogOperations.version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /** 校验目录读取权与归属后返回服务清单；未绑定资源返回空清单。 */
    public com.ingot.framework.commons.model.iam.extension.FieldBindingManifest resourceBindings(String applicationId, String resourceId) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_READ);
        operationsContext.requireApplication(IamIds.require(applicationId));
        var app = operationsContext.catalog().findApplication(IamIds.require(applicationId));
        var resource = operationsContext.catalog().findResource(IamIds.require(applicationId), IamIds.require(resourceId));
        if (resource == null || !app.getId().equals(resource.getApplicationId())) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        var key = new com.ingot.framework.commons.model.iam.extension.ResourceKey(app.getDomain(), app.getCode(), resource.getCode());
        return operationsContext.fieldMetadata().bindings(key);
    }

    /**
     * 在指定应用下创建资源。
     * @param applicationId 应用 ID
     * @param input 资源草稿
     * @return 新资源 ID
     */
    public CreatedResource createResource(String applicationId, ResourceDraft input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_CREATE);
        long appId = IamIds.require(applicationId);
        return operationsContext.transaction().execute(status -> {
            IamApplicationEntity application = CatalogOperations.requireLocked(operationsContext.catalog().lockApplication(appId));
            CatalogOperations.requireResourceScopes(application.getDomain(), input.scopeCapabilities());
            operationsContext.requireFieldCapabilities(application, input.code(), input.fieldCapabilities());
            long id = operationsContext.access().nextId();
            IamResourceEntity entity = new IamResourceEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setApplicationId(BigInteger.valueOf(appId));
            entity.setCode(input.code());
            entity.setName(input.name());
            entity.setScopeCapabilities(IamJsonCodec.array(input.scopeCapabilities()));
            entity.setFieldCapabilities(IamJsonCodec.array(input.fieldCapabilities()));
            entity.setFieldPolicy(false);
            entity.setEnabled(true);
            try {
                operationsContext.catalog().insertResource(entity);
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.RESOURCE, IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, input.name(), AuditField.FIELD_CAPABILITIES, input.fieldCapabilities()), Map.of(CatalogOperations.RESOURCE, "0"));
            operationsContext.changes().markAll();
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 更新资源名称与能力，不能改写编码或所属应用。
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<ResourceRecord> updateResource(String applicationId, String resourceId,
            ResourceUpdateInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_UPDATE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(resourceId);
        return operationsContext.transaction().execute(status -> {
            IamResourceEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockResource(appId, id));
            IamApplicationEntity application = CatalogOperations.requireLocked(operationsContext.catalog().findApplication(appId));
            CatalogOperations.requireResourceScopes(application.getDomain(), input.scopeCapabilities());
            operationsContext.requireFieldCapabilities(application, current.getCode(), input.fieldCapabilities());
            requireFieldPolicy(application, current.getCode(), input.fieldCapabilities(), input.fieldPolicy());
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            operationsContext.catalog().updateResource(appId, id, input.name(), IamJsonCodec.array(input.scopeCapabilities()),
                    IamJsonCodec.array(input.fieldCapabilities()), input.fieldPolicy(), current.getVersion());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.RESOURCE, resourceId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName(), AuditField.FIELD_CAPABILITIES, current.getFieldCapabilities() == null ? "[]" : current.getFieldCapabilities(),
                            AuditField.FIELD_POLICY, String.valueOf(Boolean.TRUE.equals(current.getFieldPolicy()))),
                    Map.of(AuditField.NAME, input.name(), AuditField.FIELD_CAPABILITIES, input.fieldCapabilities(),
                            AuditField.FIELD_POLICY, String.valueOf(input.fieldPolicy())),
                    Map.of(CatalogOperations.RESOURCE, next));
            operationsContext.changes().markAll();
            return loadResource(appId, id);
        });
    }

    /**
     * 删除未被操作引用的资源。
     * @param applicationId 应用 ID
     * @param resourceId 资源 ID
     * @return 删除前版本
     */
    public CreatedResource deleteResource(String applicationId, String resourceId) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_DELETE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(resourceId);
        return operationsContext.transaction().execute(status -> {
            IamResourceEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockResource(appId, id));
            CatalogInUse.requireUnused(operationsContext.catalog().countActions(appId, id), CatalogInUse.RESOURCE_HAS_ACTIONS);
            operationsContext.catalog().deleteResource(appId, id);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.RESOURCE, resourceId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(),
                    Map.of(CatalogOperations.RESOURCE, CatalogOperations.version(current.getVersion())));
            return new CreatedResource(resourceId, CatalogOperations.version(current.getVersion()));
        });
    }

    private ResourceDetail<ResourceRecord> loadResource(long applicationId, long id) {
        IamResourceEntity row = operationsContext.catalog().findResource(applicationId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return ResourceDetailFactory.of(resource(operationsContext.catalog().findApplication(applicationId), row), CatalogOperations.version(row.getVersion()));
    }

    private void requireFieldPolicy(IamApplicationEntity application, String resourceCode, List<FieldCapability> fields,
            boolean fieldPolicy) {
        if (!fieldPolicy) {
            return;
        }
        var key = new com.ingot.framework.commons.model.iam.extension.ResourceKey(application.getDomain(),
                application.getCode(), resourceCode);
        if (application.getDomain() != AuthorizationDomain.TENANT || fields == null || fields.isEmpty()
                || !operationsContext.fieldMetadata().eligible(key)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private ResourceRecord resource(IamApplicationEntity application, IamResourceEntity row) {
        List<ScopeKind> scopes = IamJsonCodec.read(row.getScopeCapabilities(), CatalogOperations.SCOPES);
        List<FieldCapability> fields = IamJsonCodec.read(row.getFieldCapabilities(), CatalogOperations.FIELDS);
        boolean eligible = application != null && operationsContext.fieldMetadata().eligible(
                new com.ingot.framework.commons.model.iam.extension.ResourceKey(application.getDomain(),
                        application.getCode(), row.getCode()));
        return new ResourceRecord(CatalogOperations.text(row.getId()), CatalogOperations.text(row.getApplicationId()), row.getCode(), row.getName(),
                scopes == null ? List.of() : scopes, fields == null ? List.of() : fields,
                Boolean.TRUE.equals(row.getFieldPolicy()), eligible, CatalogOperations.statusOf(row.getEnabled()));
    }
}
