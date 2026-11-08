package com.ingot.cloud.iam.application.policy;

import com.ingot.cloud.iam.infrastructure.persistence.FieldPolicyRepository;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.cloud.iam.domain.shared.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.shared.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.cloud.iam.application.policy.model.FieldOperationRule;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyDraft;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyInput;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyResource;
import com.ingot.cloud.iam.application.policy.model.FieldRule;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护已接入资源的租户字段策略，不产生业务授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class PolicyService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String FIELD_POLICY = "field-policy";
    private final IamAccess access;
    private final AuthorizationAuditWriter audits;
    private final FieldAccessEvaluator fields;
    private final ResourceFieldMetadata fieldMetadata;
    private final FieldPolicyRepository policies;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、字段求值与策略表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param fields 字段访问
     * @param policies 策略读写
     * @param transactionManager 同一数据源事务
     * @param fieldMetadata 资源字段目录
     */
    public PolicyService(IamAccess access, AuthorizationAuditWriter audits, FieldAccessEvaluator fields,
                         FieldPolicyRepository policies, PlatformTransactionManager transactionManager,
                         com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
        this.access = access;
        this.audits = audits;
        this.fields = fields;
        this.policies = policies;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 列出当前租户已接入的资源。
     *
     * @return 资源目录
     */
    public List<FieldPolicyResource> listFieldPolicyResources() {
        access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_READ);
        return fieldMetadata.fieldPolicyCatalog();
    }

    /**
     * 读取一份资源的字段策略。
     *
     * @param resourceId 资源 ID
     * @return 策略详情
     */
    public ResourceDetail<FieldPolicyDraft> getFieldPolicy(String resourceId) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_READ);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> loadField(tenantId, requireEnrolled(resourceId)));
    }

    /**
     * 整体替换一份资源的字段策略。
     *
     * @param resourceId 资源 ID
     * @param input 完整配置
     * @return 提交后详情
     */
    public ResourceDetail<FieldPolicyDraft> putFieldPolicy(String resourceId, FieldPolicyInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        var enrolled = requireEnrolled(resourceId);
        return transaction.execute(status -> {
            ResourceDetail<FieldPolicyDraft> current = loadField(tenantId, enrolled);
            IamIds.requireExpected(input.expectedVersion(), current.version());
            requireDefault(input.policy().defaultRevisionId(), enrolled.resourceId());
            replaceField(actor, tenantId, enrolled, input.policy());
            access.authorizationChanged();
            audits.write(actor.context(), access.nextId(), FIELD_POLICY, resourceId,
                    AuditChangeType.UPDATE, Map.of(AuditField.POLICY_VERSION, current.version()),
                    Map.of(AuditField.POLICY_VERSION, Long.toString(Long.parseLong(current.version()) + 1)),
                    Map.of(FIELD_POLICY, Long.toString(Long.parseLong(current.version()) + 1)));
            return loadField(tenantId, enrolled);
        });
    }

    private EnrolledResource requireEnrolled(String resourceId) {
        long id = IamIds.require(resourceId);
        return fieldMetadata.fieldPolicyCatalog().stream().filter(item -> item.resourceId().equals(resourceId)).findFirst()
                .map(item -> new EnrolledResource(id, item.resource(), fieldMetadata.requireFresh(item.resource(), false).descriptor()))
                .orElseThrow(() -> new BizException(IamReasonCode.OBJECT_NOT_FOUND));
    }

    private ResourceDetail<FieldPolicyDraft> loadField(long tenantId, EnrolledResource enrolled) {
        IamFieldPolicyEntity row = ensurePolicy(tenantId, enrolled);
        List<IamFieldRuleEntity> items = policies.listFieldRules(tenantId, enrolled.resourceId());
        LinkedHashSet<Long> selectorIds = new LinkedHashSet<>();
        for (IamFieldRuleEntity item : items) {
            selectorIds.add(selectorId(item.getViewerSelectorId()));
        }
        Map<Long, Selection> selectors = policies.selectors(tenantId, selectorIds);
        List<FieldRule> rules = new ArrayList<>();
        for (IamFieldRuleEntity item : items) {
            rules.add(new FieldRule(item.getFieldKey(),
                    selection(selectors, selectorId(item.getViewerSelectorId())),
                    IamJsonCodec.read(item.getTargetScope(), SCOPES),
                    IamJsonCodec.read(item.getScopeBindings(), BINDINGS),
                    item.getVisibility()));
        }
        return ResourceDetailFactory.of(new FieldPolicyDraft(row.getDefaultRevisionId().toString(), rules,
                IamJsonCodec.read(row.getOperationRules(), new TypeReference<List<FieldOperationRule>>() { })),
                row.getVersion().toString());
    }

    private IamFieldPolicyEntity ensurePolicy(long tenantId, EnrolledResource enrolled) {
        IamFieldPolicyEntity row = policies.findField(tenantId, enrolled.resourceId());
        if (row != null) {
            return row;
        }
        IamDefaultPolicyRevisionEntity latest = policies.latestRevision(DefaultPolicyKind.FIELD, enrolled.resourceId());
        long revisionId;
        if (latest == null) {
            revisionId = access.nextId();
            IamDefaultPolicyRevisionEntity created = new IamDefaultPolicyRevisionEntity();
            created.setId(BigInteger.valueOf(revisionId));
            created.setResourceId(BigInteger.valueOf(enrolled.resourceId()));
            created.setKind(DefaultPolicyKind.FIELD);
            created.setRevision(BigInteger.ONE);
            created.setDefinition(DefaultPolicyDefinitions.definition(enrolled.descriptor()));
            policies.insertRevision(created);
        }
        else {
            revisionId = latest.getId().longValueExact();
        }
        IamFieldPolicyEntity created = new IamFieldPolicyEntity();
        created.setTenantId(BigInteger.valueOf(tenantId));
        created.setResourceId(BigInteger.valueOf(enrolled.resourceId()));
        created.setDefaultRevisionId(BigInteger.valueOf(revisionId));
        created.setDefaultKind(DefaultPolicyKind.FIELD);
        created.setOperationRules("[]");
        created.setVersion(BigInteger.ZERO);
        policies.insertField(created);
        return policies.findField(tenantId, enrolled.resourceId());
    }

    private void replaceField(ActiveIdentity actor, long tenantId, EnrolledResource enrolled, FieldPolicyDraft policy) {
        policies.deleteFieldRules(tenantId, enrolled.resourceId());
        for (var operation : policy.operationRules()) {
            requireOperationRule(enrolled, operation);
            writeSelector(actor, tenantId, operation.viewerSelection());
        }
        policies.upsertField(tenantId, enrolled.resourceId(), IamIds.require(policy.defaultRevisionId()),
                IamJsonCodec.array(policy.operationRules()));
        for (FieldRule rule : policy.rules()) {
            requireVisibilityRule(enrolled.descriptor(), rule);
            long viewer = writeSelector(actor, tenantId, rule.viewerSelection());
            policies.insertFieldRule(access.nextId(), tenantId, enrolled.resourceId(), rule.fieldKey(), viewer,
                    IamJsonCodec.array(rule.targetScope()), IamJsonCodec.object(rule.scopeBindings()), rule.visibility());
        }
    }

    private void requireVisibilityRule(ResourceDescriptor descriptor, FieldRule rule) {
        FieldCapability field = descriptor.fields().stream().filter(value -> value.key().equals(rule.fieldKey())).findFirst()
                .orElseThrow(() -> new BizException(IamReasonCode.INVALID_ARGUMENT));
        if (!field.visibilities().contains(rule.visibility())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private void requireOperationRule(EnrolledResource enrolled, FieldOperationRule rule) {
        var descriptor = enrolled.descriptor();
        var manifest = fieldMetadata.bindings(enrolled.resource());
        var write = manifest.bindings().stream().anyMatch(binding -> rule.actionCode().equals(binding.actionCode())
                && rule.fieldKey().equals(binding.fieldKey()) && binding.use() == com.ingot.framework.commons.annotation.field.FieldUse.WRITE);
        var filter = manifest.bindings().stream().anyMatch(binding -> rule.actionCode().equals(binding.actionCode())
                && rule.fieldKey().equals(binding.fieldKey()) && binding.use() == com.ingot.framework.commons.annotation.field.FieldUse.FILTER);
        var action = descriptor.actions().stream().filter(value -> value.code().equals(rule.actionCode()))
                .findFirst().orElseThrow(() -> new BizException(IamReasonCode.INVALID_ARGUMENT));
        var field = descriptor.fields().stream().filter(value -> value.key().equals(rule.fieldKey()))
                .findFirst().orElseThrow(() -> new BizException(IamReasonCode.INVALID_ARGUMENT));
        if (rule.operations().editable() && (!write || !field.editable()
                    || action.mode() != com.ingot.framework.commons.model.iam.extension.ExecutionMode.MUTATING)
                || rule.operations().filterable() && (!filter || !field.filterable()
                    || action.mode() != com.ingot.framework.commons.model.iam.extension.ExecutionMode.READ_ONLY)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private long writeSelector(ActiveIdentity actor, long tenantId, Selection selection) {
        IamSelections.requireCompatible(AuthorizationDomain.TENANT, selection);
        long id = access.nextId();
        policies.insertSelector(id, tenantId);
        for (String memberId : selection.members()) {
            policies.insertSelectorMember(tenantId, id, IamIds.require(memberId));
        }
        for (DepartmentSelection department : selection.departments()) {
            policies.insertSelectorDepartment(tenantId, id, IamIds.require(department.id()),
                    department.includeDescendants());
        }
        return id;
    }

    private void requireDefault(String revisionId, long resourceId) {
        var revision = policies.findRevision(IamIds.require(revisionId), DefaultPolicyKind.FIELD);
        if (revision == null || revision.getResourceId() == null || revision.getResourceId().longValueExact() != resourceId) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private record EnrolledResource(long resourceId, ResourceKey resource, ResourceDescriptor descriptor) {
    }

    private static Selection selection(Map<Long, Selection> selectors, long selectorId) {
        return selectors.getOrDefault(selectorId, new Selection(List.of(), List.of()));
    }

    private static long selectorId(BigInteger value) {
        return value == null ? 0L : value.longValueExact();
    }
}
