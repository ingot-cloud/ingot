package com.ingot.cloud.iam.application.delegation;

import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.AssignmentDurationMode;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.delegation.model.ResourceScopeCeiling;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationInput;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationRecord;
import com.ingot.cloud.iam.application.role.model.ScopeIssue;
import com.ingot.cloud.iam.application.shared.model.ObjectCapability;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import com.ingot.cloud.iam.domain.role.ScopeIssueReason;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>提供delegation用例共享的持久化装配与内部规则，事务边界由各用例保持。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantDelegationOperations {

    static final String TARGET="delegation";
    static final TypeReference<Map<String,ScopeBinding>> BINDINGS=new TypeReference<>() { };
    static final TypeReference<List<ScopeExpression>> SCOPES=new TypeReference<>() { };
    private final IamAccess access;
    private final TenantAdministrationMapper live;
    private final TenantRoleDefinitionRepository roles;
    private final TenantRoleDefinitionResolver roleDefinitions;
    private final TenantRoleLockService roleLocks;
    private final TenantScopeBindingService scopeBindings;
    private final ResourceScopeAccessService resources;
    private final TenantAuthorizationTransaction transactions;
    private final AuthorizationAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    /** 不含联系方式或业务值的委派授权事实。 */
    record AuditScope(String administratorMemberId,List<String> allowedRoleIds,List<ResourceScopeCeiling> resourceScopeCeilings,String maxAssignmentDuration) { }
    IamAccess access() { return access; }

    TenantAdministrationMapper live() { return live; }

    TenantRoleDefinitionRepository roles() { return roles; }

    TenantRoleDefinitionResolver roleDefinitions() { return roleDefinitions; }

    TenantRoleLockService roleLocks() { return roleLocks; }

    TenantScopeBindingService scopeBindings() { return scopeBindings; }

    ResourceScopeAccessService resources() { return resources; }

    TenantAuthorizationTransaction transactions() { return transactions; }

    AuthorizationAuditWriter audits() { return audits; }

    AuthorizationChangeNotifier changes() { return changes; }

    void validate(ActiveIdentity actor,TenantDelegationInput input){if(input.assignmentDurationMode()!=AssignmentDurationMode.LIMITED||input.maxAssignmentDuration()==null||input.maxAssignmentDuration().isNegative()||input.maxAssignmentDuration().isZero())invalid();
        if(live.members(tenant(actor),List.of(new BigInteger(input.administratorMemberId())))!=1)missing();
        if(input.allowedRoleIds().isEmpty()||input.allowedRoleIds().stream().distinct().count()!=input.allowedRoleIds().size()||input.resourceScopeCeilings().stream().map(ResourceScopeCeiling::resourceId).distinct().count()!=input.resourceScopeCeilings().size())invalid();
        Set<BigInteger> required=new HashSet<>();for(var id:input.allowedRoleIds()){var role=live.role(tenant(actor),new BigInteger(id));if(role==null)missing();if(!Boolean.TRUE.equals(role.enabled()))throw new BizException(IamReasonCode.ROLE_UNAVAILABLE);var def=roleDefinitions.activeDefinition(actor,role);if(!def.grants().isEmpty())live.actions(def.grants().stream().map(g->new BigInteger(g.actionId())).toList()).forEach(a->required.add(a.resourceId()));}
        var ceilings=input.resourceScopeCeilings().stream().map(c->new BigInteger(c.resourceId())).collect(java.util.stream.Collectors.toSet());if(!ceilings.containsAll(required))invalid();
        for(var c:input.resourceScopeCeilings()){if(live.resource(new BigInteger(c.resourceId()))==null)invalid();var supported=IamJsonCodec.read(live.resourceScopeKinds(new BigInteger(c.resourceId())), new TypeReference<List<ScopeKind>>() { }); List<CurrentRoleParameter> parameters=new ArrayList<>();for(var scope:c.scopes()){if(!supported.contains(scope.kind()))invalid();if(scope.parameterKey()!=null)parameters.add(new CurrentRoleParameter(scope.parameterKey(),scope.kind()==ScopeKind.MANAGED_DEPARTMENTS?ScopeBindingKind.DEPARTMENTS:ScopeBindingKind.OBJECTS,c.resourceId()));}scopeBindings.validateBindings(actor,parameters.stream().distinct().toList(),c.scopeBindings());}
        var selection=input.recipientSelection();if(selection==null||selection.members().isEmpty()&&selection.departments().isEmpty())invalid();
        if(selection.members().contains(input.administratorMemberId()))invalid();for(var id:selection.members())if(live.members(tenant(actor),List.of(new BigInteger(id)))!=1)missing();
        for(var d:selection.departments())if(live.departmentExists(tenant(actor),new BigInteger(d.id()))!=1)missing();
    }
    List<ScopeIssue> issues(ActiveIdentity actor,TenantDelegationInput input){var available=input.resourceScopeCeilings().stream().map(ResourceScopeCeiling::resourceId).collect(java.util.stream.Collectors.toSet());List<ScopeIssue> issues=new ArrayList<>();for(var id:input.allowedRoleIds()){var role=live.role(tenant(actor),new BigInteger(id));if(role==null)continue;var def=roleDefinitions.activeDefinition(actor,role);if(def.grants().isEmpty())continue;for(var a:live.actions(def.grants().stream().map(g->new BigInteger(g.actionId())).toList()))if(!available.contains(a.resourceId().toString()))issues.add(new ScopeIssue(a.resourceId().toString(),List.of(a.actionId().toString()),null,ScopeIssueReason.MISSING_RESOURCE_CEILING));}return issues;}
    TenantDelegationInput input(ActiveIdentity actor,TenantAdministrationRows.Delegation d){return new TenantDelegationInput(d.administratorMemberId().toString(),live.delegatedRoles(d.id()).stream().map(BigInteger::toString).toList(),new Selection(live.recipientMembers(tenant(actor),d.id()).stream().map(BigInteger::toString).toList(),live.recipientDepartments(tenant(actor),d.id())),live.ceilings(d.id()).stream().map(c->new ResourceScopeCeiling(c.resourceId().toString(),IamJsonCodec.read(c.scopes(),SCOPES),IamJsonCodec.read(c.scopeBindings(),BINDINGS))).toList(),instant(d.validFrom()),instant(d.validUntil()),Duration.ofSeconds(d.durationSeconds().longValueExact(),d.durationNanos()),AssignmentDurationMode.LIMITED);}
    ResourceDetail<TenantDelegationRecord> detail(ActiveIdentity actor,TenantAdministrationRows.Delegation d){var input=input(actor,d);var capabilities=new LinkedHashMap<String,ObjectCapability>();
        var allowed=access.capabilities(actor,List.of(IamAction.TENANT_DELEGATION_READ,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_DELEGATION_DELETE,IamAction.TENANT_DELEGATION_PREVIEW));
        for(var operation:List.of(IamAction.TENANT_DELEGATION_READ,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_DELEGATION_DELETE,IamAction.TENANT_DELEGATION_PREVIEW)) {
            boolean grant=allowed.allows(operation,true) && com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql.matches(resources.objects(actor.context(),operation),d.id().longValueExact());
            if(operation==IamAction.TENANT_DELEGATION_UPDATE&&d.status()!=GrantStatus.ACTIVE)grant=false;
            capabilities.put(operation.getCode(),new ObjectCapability(grant,grant?null:IamReasonCode.ACTION_DENIED,grant?null:"当前对象不允许此操作"));
        }
        return ResourceDetailFactory.of(new TenantDelegationRecord(d.id().toString(),input,d.status(),d.administratorName(),instant(d.createdAt()),issues(actor,input)),capabilities,d.version().toString());}
    TenantAdministrationRows.Delegation require(ActiveIdentity actor,String id,boolean lock){var d=lock?live.lockDelegation(tenant(actor),new BigInteger(id)):live.delegation(tenant(actor),new BigInteger(id));if(d==null)missing();return d;}

    static BigInteger tenant(ActiveIdentity actor){return new BigInteger(actor.context().tenantId());}
    static Instant instant(LocalDateTime i){return i==null?null:i.toInstant(ZoneOffset.UTC);}
    static void invalid(){throw new BizException(IamReasonCode.INVALID_ARGUMENT);}
    static void missing(){throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);}
}
