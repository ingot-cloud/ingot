package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.FieldOperationRule;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldPolicyInput;
import com.ingot.framework.commons.model.iam.FieldRule;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberResources;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护租户成员字段策略，不产生业务授权。</p>
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
    private final IamAuditWriter audits;
    private final FieldAccessEvaluator fields;
    private final com.ingot.cloud.iam.extension.ResourceFieldMetadata fieldMetadata;
    private final PolicyWriteRepository policies;
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
    public PolicyService(IamAccess access, IamAuditWriter audits, FieldAccessEvaluator fields,
                         PolicyWriteRepository policies, PlatformTransactionManager transactionManager,
                         com.ingot.cloud.iam.extension.ResourceFieldMetadata fieldMetadata) {
        this.fieldMetadata = fieldMetadata;
        this.access = access;
        this.audits = audits;
        this.fields = fields;
        this.policies = policies;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 读取字段策略。
     *
     * @return 策略详情
     */
    public ResourceDetail<FieldPolicyDraft> getFieldPolicy() {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_READ);
        return loadField(IamIds.require(actor.context().tenantId()));
    }

    /**
     * 整体替换字段策略。
     *
     * @param input 完整配置
     * @return 提交后详情
     */
    public ResourceDetail<FieldPolicyDraft> putFieldPolicy(FieldPolicyInput input) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_FIELD_POLICY_UPDATE);
        long tenantId = IamIds.require(actor.context().tenantId());
        return transaction.execute(status -> {
            ResourceDetail<FieldPolicyDraft> current = loadField(tenantId);
            IamIds.requireExpected(input.expectedVersion(), current.version());
            requireDefault(input.policy().defaultRevisionId(), DefaultPolicyKind.FIELD);
            replaceField(actor, tenantId, input.policy());
            access.authorizationChanged();
            audits.write(actor.context(), access.nextId(), FIELD_POLICY, IamIds.text(tenantId),
                    AuditChangeType.UPDATE, Map.of(AuditField.POLICY_VERSION, current.version()),
                    Map.of(AuditField.POLICY_VERSION, Long.toString(Long.parseLong(current.version()) + 1)),
                    Map.of(FIELD_POLICY, Long.toString(Long.parseLong(current.version()) + 1)));
            return loadField(tenantId);
        });
    }

    private ResourceDetail<FieldPolicyDraft> loadField(long tenantId) {
        IamFieldPolicyEntity row = policies.findField(tenantId);
        if (row == null) {
            return IamDetails.of(new FieldPolicyDraft(latestDefault(DefaultPolicyKind.FIELD), List.of()), "0");
        }
        List<IamFieldRuleEntity> items = policies.listFieldRules(tenantId);
        LinkedHashSet<Long> selectorIds = new LinkedHashSet<>();
        for (IamFieldRuleEntity item : items) {
            selectorIds.add(selectorId(item.getViewerSelectorId()));
        }
        Map<Long, Selection> selectors = policies.selectors(tenantId, selectorIds);
        List<FieldRule> rules = new ArrayList<>();
        for (IamFieldRuleEntity item : items) {
            rules.add(new FieldRule(item.getScenario(), item.getFieldKey(),
                    selection(selectors, selectorId(item.getViewerSelectorId())),
                    IamJson.read(item.getTargetScope(), SCOPES),
                    IamJson.read(item.getScopeBindings(), BINDINGS),
                    item.getVisibility()));
        }
        return IamDetails.of(new FieldPolicyDraft(row.getDefaultRevisionId().toString(), rules,
                IamJson.read(row.getOperationRules(), new TypeReference<List<FieldOperationRule>>() { })),
                row.getVersion().toString());
    }

    private void replaceField(ActiveIdentity actor, long tenantId, FieldPolicyDraft policy) {
        policies.deleteFieldRules(tenantId);
        for (var operation : policy.operationRules()) {
            requireOperationRule(operation);
            writeSelector(actor, tenantId, operation.viewerSelection());
        }
        policies.upsertField(tenantId, IamIds.require(policy.defaultRevisionId()), IamJson.array(policy.operationRules()));
        for (FieldRule rule : policy.rules()) {
            requireVisibilityRule(rule);
            long viewer = writeSelector(actor, tenantId, rule.viewerSelection());
            policies.insertFieldRule(access.nextId(), tenantId, rule.scenario(), rule.fieldKey(), viewer,
                    IamJson.array(rule.targetScope()), IamJson.object(rule.scopeBindings()), rule.visibility());
        }
    }

    private void requireVisibilityRule(FieldRule rule) {
        var field = fieldMetadata.requireFresh(MemberResources.TENANT_MEMBER, false).descriptor().fields().stream()
                .filter(value -> value.key().equals(rule.fieldKey())).findFirst()
                .orElseThrow(() -> new BizException(IamReasonCode.INVALID_ARGUMENT));
        if (!field.visibilities().contains(rule.visibility())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private void requireOperationRule(FieldOperationRule rule) {
        if (rule.resource().domain() != AuthorizationDomain.TENANT
                || !MemberResources.TENANT_MEMBER.equals(rule.resource())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var descriptor = fieldMetadata.requireFresh(rule.resource(), false).descriptor();
        var manifest = fieldMetadata.bindings(rule.resource());
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

    private void requireDefault(String revisionId, DefaultPolicyKind kind) {
        if (policies.findRevision(IamIds.require(revisionId), kind) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private String latestDefault(DefaultPolicyKind kind) {
        var revision = policies.latestRevision(kind);
        if (revision == null || revision.getId() == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return revision.getId().toString();
    }

    private static Selection selection(Map<Long, Selection> selectors, long selectorId) {
        return selectors.getOrDefault(selectorId, new Selection(List.of(), List.of()));
    }

    private static long selectorId(BigInteger value) {
        return value == null ? 0L : value.longValueExact();
    }
}
