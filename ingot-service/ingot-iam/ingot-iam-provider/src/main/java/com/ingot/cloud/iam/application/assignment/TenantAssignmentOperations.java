package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.infrastructure.persistence.TenantAuthorizationOptionSql;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.cloud.iam.application.assignment.model.SubjectRef;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentRecord;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.ScopeIssue;
import com.ingot.cloud.iam.application.shared.model.ObjectCapability;
import com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.assignment.TenantAssignmentOptionKind;
import com.ingot.cloud.iam.domain.role.CurrentRoleDefinition;
import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import com.ingot.cloud.iam.domain.role.ScopeIssueReason;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>提供assignment用例共享的持久化装配与内部规则，事务边界由各用例保持。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantAssignmentOperations {

    private final TenantRoleDefinitionResolver roleDefinitions;
    private final TenantRoleLockService roleLocks;
    private final TenantScopeBindingService scopeBindings;

    static final String TARGET = "assignment";
    static final String SCOPE_FIELD = "scopeBindings";
    static final TypeReference<Map<String,ScopeBinding>> BINDINGS = new TypeReference<>() { };
    static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() { };
    static final TypeReference<List<String>> SOURCE_TYPES = new TypeReference<>() { };
    private final IamAccess access;
    private final TenantAdministrationMapper live;
    private final TenantRoleWorkspaceMapper workspace;
    private final TenantRoleDefinitionRepository roles;
    private final TenantRoleDefinitionService roleService;
    private final ResourceScopeAccessService resources;
    private final TenantAuthorizationTransaction transactions;
    private final AuthorizationAuditWriter audits;
    private final AuthorizationChangeNotifier changes;

    private final com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments memberDepartments;
    TenantRoleDefinitionResolver roleDefinitions() { return roleDefinitions; }

    TenantRoleLockService roleLocks() { return roleLocks; }

    TenantScopeBindingService scopeBindings() { return scopeBindings; }

    IamAccess access() { return access; }

    TenantAdministrationMapper live() { return live; }

    TenantRoleWorkspaceMapper workspace() { return workspace; }

    TenantRoleDefinitionRepository roles() { return roles; }

    TenantRoleDefinitionService roleService() { return roleService; }

    ResourceScopeAccessService resources() { return resources; }

    TenantAuthorizationTransaction transactions() { return transactions; }

    AuthorizationAuditWriter audits() { return audits; }

    AuthorizationChangeNotifier changes() { return changes; }

    com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments memberDepartments() { return memberDepartments; }

    /** 校验当前完整绑定及持续委派约束，绝不自动扩大参数。 */
    void validate(ActiveIdentity actor,TenantAssignmentInput input,IamAction action) {
        var role=role(actor,input.roleId()); if(!Boolean.TRUE.equals(role.enabled()))throw new BizException(IamReasonCode.ROLE_UNAVAILABLE); var definition=activeDefinition(actor,role);
        var grant=source(actor,input.delegationGrantId(),action);
        if(grant==null&&action==IamAction.TENANT_ASSIGNMENT_CREATE&&!resources.objects(actor.context(),action).coversAll())denied();
        if(grant!=null&&!live.delegatedRoles(grant.id()).contains(role.id()))exceeded();
        if(input.subject().type()==SubjectType.MEMBER){if(live.members(tenant(actor),List.of(new BigInteger(input.subject().id())))!=1)missing();}
        else if(live.groupExists(tenant(actor),new BigInteger(input.subject().id()))!=1)missing();
        validateBindings(actor,definition.parameterDefinitions(),input.scopeBindings());
        if(grant!=null){var recipients=input.subject().type()==SubjectType.MEMBER?List.of(new BigInteger(input.subject().id())):live.groupMembers(tenant(actor),new BigInteger(input.subject().id()));
            if(input.subject().type()==SubjectType.GROUP
                ? !live.groupWithinSource(new TenantAuthorizationOptionSql.Query(tenant(actor),grant.administratorMemberId(),grant.id(),TenantAssignmentOptionKind.GROUP,"%",List.of(),1,0),new BigInteger(input.subject().id()))
                : recipients.isEmpty()||recipients.stream().anyMatch(id->id.equals(member(actor))||live.recipient(tenant(actor),grant.id(),id)==0))exceeded();
            Instant from=input.validFrom()==null?Instant.now():input.validFrom(),until=input.validUntil();
            if(until==null||!until.isAfter(from)||Duration.between(from,until).compareTo(Duration.ofSeconds(grant.durationSeconds().longValueExact(),grant.durationNanos()))>0
                ||grant.validFrom()!=null&&from.isBefore(instant(grant.validFrom()))||grant.validUntil()!=null&&until.isAfter(instant(grant.validUntil())))exceeded();
            var limits=live.ceilings(grant.id());var actions=definition.grants().isEmpty()?List.<TenantAdministrationRows.Action>of():live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList());
            for(var g:definition.grants()){var a=actions.stream().filter(v->v.actionId().toString().equals(g.actionId())).findFirst().orElseThrow(()->new BizException(IamReasonCode.INVALID_ARGUMENT));
                var limit=limits.stream().filter(c->c.resourceId().equals(a.resourceId())).findFirst().orElse(null);if(limit==null)exceeded();
                // 固定角色ALL/SELF声明由资源上限持续收缩；仅拒绝操作者显式配置的越界绑定。
                for(var expression:g.scopes()) {
                    if(expression.parameterKey()==null)continue;
                    var parameter=definition.parameterDefinitions().stream().filter(p->p.key().equals(expression.parameterKey())).findFirst().orElseThrow();
                    var allowed=scopeBindings.allowedBindings(actor,parameter,grant);
                    if(allowed!=null && input.scopeBindings().get(parameter.key()).ids().stream().map(BigInteger::new).anyMatch(id->!allowed.contains(id)))exceeded();
                }}
        }
    }

    /** 按资源真实归属校验全部命名绑定。 */
    void validateBindings(ActiveIdentity actor,List<CurrentRoleParameter> parameters,Map<String,ScopeBinding> bindings) {
        scopeBindings.validateBindings(actor, parameters, bindings);
    }

    /** 本人有效来源；独立资格可不选来源。 */
    TenantAdministrationRows.Delegation source(ActiveIdentity actor,String id,IamAction action) {
        boolean governed=action==null?(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_PREVIEW,true))
            :access.allows(AuthorizationDomain.TENANT,action,true);
        if(id==null||id.isBlank()){if(!governed)denied();return null;}
        var source=live.mine(tenant(actor),member(actor)).stream().filter(d->d.id().toString().equals(id)).findFirst().orElseThrow(()->new BizException(IamReasonCode.DELEGATION_EXCEEDED));
        return source;
    }

    /** 计算当前定义待补参数或资源上限，旧有效资源持续可用。 */
    List<ScopeIssue> issues(ActiveIdentity actor,TenantAdministrationRows.Role role,Map<String,ScopeBinding> bindings,BigInteger source) {
        var definition=activeDefinition(actor,role);List<ScopeIssue> issues=new ArrayList<>();
        for(var p:definition.parameterDefinitions()){var b=bindings.get(p.key());if(b==null||b.kind()!=p.kind())issues.add(new ScopeIssue(p.resourceId(),
            definition.grants().stream().filter(g->g.scopes().stream().anyMatch(s->p.key().equals(s.parameterKey()))).map(ActionGrant::actionId).toList(),p.key(),b==null?ScopeIssueReason.MISSING_BINDING:ScopeIssueReason.INVALID_BINDING));}
        if(source!=null&&!definition.grants().isEmpty()){var limits=live.ceilings(source).stream().map(TenantAdministrationRows.Ceiling::resourceId).collect(java.util.stream.Collectors.toSet());
            for(var a:live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList()))if(!limits.contains(a.resourceId()))issues.add(new ScopeIssue(a.resourceId().toString(),List.of(a.actionId().toString()),null,ScopeIssueReason.MISSING_RESOURCE_CEILING));}
        return List.copyOf(issues);
    }

    /** 分配仅配置本组织当前可执行资源；未开通或停用资源不制造不可见必填参数。 */
    CurrentRoleDefinition activeDefinition(ActiveIdentity actor,TenantAdministrationRows.Role role) {
        return roleDefinitions.activeDefinition(actor, role);
    }

    ResourceDetail<TenantAssignmentRecord> detail(ActiveIdentity actor,TenantAdministrationRows.Assignment row) {
        var input=new TenantAssignmentInput(new SubjectRef(row.subjectType(),(row.subjectType()==SubjectType.MEMBER?row.memberId():row.groupId()).toString()),row.roleId().toString(),IamJsonCodec.read(row.scopeBindings(),BINDINGS),instant(row.validFrom()),instant(row.validUntil()),row.delegationGrantId()==null?null:row.delegationGrantId().toString());
        var role=role(actor,row.roleId().toString());var state=row.status()==GrantStatus.REVOKED?AssignmentEffectiveStatus.REVOKED:
            row.validFrom().isAfter(LocalDateTime.now(ZoneOffset.UTC))?AssignmentEffectiveStatus.PENDING:row.validUntil()!=null&&!row.validUntil().isAfter(LocalDateTime.now(ZoneOffset.UTC))?AssignmentEffectiveStatus.EXPIRED:AssignmentEffectiveStatus.ACTIVE;
        if((state==AssignmentEffectiveStatus.ACTIVE||state==AssignmentEffectiveStatus.PENDING)&&(!Boolean.TRUE.equals(role.enabled())||row.delegationGrantId()!=null&&sourceInvalid(actor,row)))state=AssignmentEffectiveStatus.SOURCE_INVALID;
        var record=new TenantAssignmentRecord(row.id().toString(),input,row.status(),row.source(),row.subjectName(),row.roleName(),row.roleKind(),row.delegationGrantId()==null?null:row.delegationGrantId().toString(),instant(row.createdAt()),null,state,issues(actor,role,input.scopeBindings(),row.delegationGrantId()));
        Map<String,ObjectCapability> capabilities=new LinkedHashMap<>();
        var available=access.capabilities(actor,List.of(IamAction.TENANT_ASSIGNMENT_UPDATE,IamAction.TENANT_ASSIGNMENT_DELETE));
        var ownSources=live.mine(tenant(actor),member(actor)).stream().map(TenantAdministrationRows.Delegation::id).collect(java.util.stream.Collectors.toSet());
        for(var operation:List.of(IamAction.TENANT_ASSIGNMENT_UPDATE,IamAction.TENANT_ASSIGNMENT_DELETE)) {
            boolean allowed=available.allows(operation,false);
            if(allowed) allowed=available.allows(operation,true)
                ? ObjectScopeSql.matches(resources.objects(actor.context(),operation),row.id().longValueExact())
                : row.delegationGrantId()!=null && ownSources.contains(row.delegationGrantId());
            if(operation==IamAction.TENANT_ASSIGNMENT_UPDATE && row.status()!=GrantStatus.ACTIVE)allowed=false;
            capabilities.put(operation.getCode(),new ObjectCapability(allowed,allowed?null:IamReasonCode.ACTION_DENIED,allowed?null:"当前来源或对象范围不允许"));
        }
        return ResourceDetailFactory.of(record,capabilities,row.version().toString());
    }
    boolean sourceInvalid(ActiveIdentity actor,TenantAdministrationRows.Assignment row) {
        var source=live.delegation(tenant(actor),row.delegationGrantId());var now=LocalDateTime.now(ZoneOffset.UTC);
        if(source==null||source.status()!=GrantStatus.ACTIVE||!live.delegatedRoles(source.id()).contains(row.roleId())
            ||source.validFrom()!=null&&source.validFrom().isAfter(now)||source.validUntil()!=null&&!source.validUntil().isAfter(now)
            ||live.members(tenant(actor),List.of(source.administratorMemberId()))!=1||row.validUntil()==null
            ||source.validFrom()!=null&&row.validFrom().isBefore(source.validFrom())||source.validUntil()!=null&&row.validUntil().isAfter(source.validUntil())
            ||Duration.between(row.validFrom(),row.validUntil()).compareTo(Duration.ofSeconds(source.durationSeconds().longValueExact(),source.durationNanos()))>0)return true;
        return row.subjectType()==SubjectType.GROUP
            ? !live.groupWithinSource(new TenantAuthorizationOptionSql.Query(tenant(actor),source.administratorMemberId(),source.id(),TenantAssignmentOptionKind.GROUP,"%",List.of(),1,0),row.groupId())
            : live.recipient(tenant(actor),source.id(),row.memberId())==0;
    }
    void validateBatchSource(ActiveIdentity actor,TenantAssignmentBatchInput batch,IamAction operation){if(batch.items()==null||batch.items().isEmpty())invalid();if(!access.allows(AuthorizationDomain.TENANT,operation,true)&&batch.items().stream().map(TenantAssignmentInput::delegationGrantId).distinct().count()!=1)exceeded();}
    TenantAdministrationRows.Role role(ActiveIdentity actor,String id){
        var role=org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
            ? live.lockRole(new BigInteger(id)) : live.role(tenant(actor),new BigInteger(id));
        if(role==null || role.kind()!=RoleKind.SHARED && (role.kind()!=RoleKind.TENANT_CUSTOM || !tenant(actor).equals(role.tenantId())))missing();return role;
    }
    static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
    static BigInteger member(ActiveIdentity a){return new BigInteger(a.context().memberId());}
    static Instant instant(LocalDateTime i){return i==null?null:i.toInstant(ZoneOffset.UTC);}
    static void invalid(){throw new BizException(IamReasonCode.INVALID_ARGUMENT);}
    static void missing(){throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);}
    static void denied(){throw new BizException(IamReasonCode.ACTION_DENIED);}
    static void exceeded(){throw new BizException(IamReasonCode.DELEGATION_EXCEEDED);}
}
