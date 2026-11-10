package com.ingot.cloud.iam.application.catalog;

import com.ingot.cloud.iam.application.asset.IamImageBinding;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.infrastructure.persistence.CatalogRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.SensitiveConfirmationGuard;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.application.catalog.model.GrantCatalogAction;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.catalog.MenuDraft;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>提供catalog用例共享的持久化装配与内部规则，事务边界由各用例保持。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
public class CatalogOperations {

    static final TypeReference<List<ScopeKind>> SCOPES = new TypeReference<>() {
    };

    static final TypeReference<List<FieldCapability>> FIELDS = new TypeReference<>() {
    };

    static final String APPLICATION = "application";
    static final String DUPLICATE_APPLICATION_CODE = "应用编码已存在，请更换应用编码";

    static final String RESOURCE = "resource";

    static final String ACTION = "action";

    static final String MENU = "menu";

    static final String PLAN = "plan";

    static final String GOVERNANCE_PLATFORM_CODE = "iam-platform";

    static final String GOVERNANCE_TENANT_CODE = "iam-tenant";

    static final String GOVERNANCE_PURGE_DENIED = "治理应用不可强制清除";

    private final IamAccess access;

    private final AuthorizationAuditWriter audits;

    private final AuthorizationChangeNotifier changes;

    private final CatalogRepository catalog;

    private final SensitiveConfirmationGuard confirmations;

    private final TransactionTemplate transaction;
    private final IamImageBinding images;

    private final com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata;

    /**
     * 绑定身份、审计、确认、目录库与事务。
     * <p>
     * TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。
     * </p>
     * @param access 当前身份与 ACTION
     * @param audits 同事务审计
     * @param changes 应用/操作启停后的授权失效
     * @param catalog 目录持久化
     * @param confirmations 写操作内嵌身份确认
     * @param transactionManager 同一数据源事务
     */
    public CatalogOperations(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
            CatalogRepository catalog, SensitiveConfirmationGuard confirmations,
            com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata,
            IamImageBinding images, PlatformTransactionManager transactionManager) {
        this.fieldMetadata = fieldMetadata;
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.catalog = catalog;
        this.confirmations = confirmations;
        this.transaction = new TransactionTemplate(transactionManager);
        this.images = images;
    }
    IamAccess access() { return access; }

    AuthorizationAuditWriter audits() { return audits; }

    AuthorizationChangeNotifier changes() { return changes; }

    CatalogRepository catalog() { return catalog; }

    SensitiveConfirmationGuard confirmations() { return confirmations; }

    TransactionTemplate transaction() { return transaction; }

    IamImageBinding images() { return images; }

    com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata() { return fieldMetadata; }

    void requireApplication(long id) {
        if (!catalog.existsApplication(id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    static void requireBaselineDomain(AuthorizationDomain domain, boolean baseline) {
        if (domain == AuthorizationDomain.PLATFORM && baseline) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "平台应用不能设置为租户默认开通应用");
        }
    }

    static void requireResourceScopes(AuthorizationDomain domain, List<ScopeKind> scopes) {
        if (domain == AuthorizationDomain.PLATFORM && scopes != null && scopes.stream()
            .anyMatch(scope -> scope == ScopeKind.MEMBER_DEPARTMENTS || scope == ScopeKind.MANAGED_DEPARTMENTS)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "平台应用不支持部门范围");
        }
    }

    static <T> T requireLocked(T row) {
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return row;
    }

    void requireFieldCapabilities(IamApplicationEntity application, String resourceCode, List<FieldCapability> fields) {
        if (fields == null) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        Set<String> keys = new HashSet<>();
        for (var field : fields) if (!keys.add(field.key())) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var key = new com.ingot.framework.commons.model.iam.extension.ResourceKey(application.getDomain(), application.getCode(), resourceCode);
        var manifest = fieldMetadata.bindings(key);
        if (manifest == null) throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        for (var field : fields) {
            if (field.visibilities().contains(com.ingot.framework.commons.model.iam.FieldVisibility.MASKED)
                    && manifest.bindings().stream().anyMatch(binding -> binding.use() == com.ingot.framework.commons.annotation.field.FieldUse.READ
                            && field.key().equals(binding.fieldKey()) && !binding.textual()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    void replaceMenuActions(long applicationId, long menuId, List<String> actionIds) {
        List<Long> ids = new ArrayList<>();
        if (actionIds != null) {
            for (String actionId : actionIds) {
                long id = IamIds.require(actionId);
                if (!catalog.existsAction(applicationId, id)) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND.getCode(), "菜单绑定的操作不存在或不可访问，请重新选择操作");
                }
                ids.add(id);
            }
        }
        catalog.replaceMenuActions(applicationId, menuId, actionIds == null ? null : ids);
    }

    static IamMenuEntity menuEntity(long id, long applicationId, Long parentId, MenuDraft input,
            boolean enabled) {
        IamMenuEntity entity = new IamMenuEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setApplicationId(BigInteger.valueOf(applicationId));
        entity.setParentId(parentId == null ? null : BigInteger.valueOf(parentId));
        entity.setName(input.name());
        entity.setPath(nullable(input.path()));
        entity.setViewPath(nullable(input.viewPath()));
        entity.setRouteName(nullable(input.routeName()));
        entity.setIcon(nullable(input.icon()));
        entity.setHidden(input.hidden());
        entity.setIsCache(input.isCache());
        entity.setProps(input.props());
        entity.setRouteParams(IamJsonCodec.array(input.routeParams()));
        entity.setKind(input.kind());
        entity.setMatchMode(input.matchMode());
        entity.setAccessMode(input.accessMode());
        entity.setSortOrder(input.sortOrder());
        entity.setEnabled(enabled);
        return entity;
    }

    GrantCatalogAction grantCatalogAction(IamActionEntity row) {
        return new GrantCatalogAction(text(row.getId()), row.getCode(), row.getName());
    }

    static ConfigurationStatus statusOf(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED;
    }

    static String nextVersion(BigInteger current) {
        return current.add(BigInteger.ONE).toString();
    }

    static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    static List<String> texts(List<BigInteger> ids) {
        return ids.stream().map(CatalogOperations::text).toList();
    }

    static String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
