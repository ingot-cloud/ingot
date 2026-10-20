package com.ingot.cloud.iam.delegation;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.assignment.TenantLiveAssignmentService;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.TenantCurrentRoleRepository;
import com.ingot.cloud.iam.persistence.mapper.TenantLiveMapper;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>租户委派角色身份与资源范围上限，持续约束派生分配。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantLiveDelegationService {
    private static final String TARGET="delegation";
    private static final TypeReference<Map<String,ScopeBinding>> BINDINGS=new TypeReference<>() { };
    private static final TypeReference<List<ScopeExpression>> SCOPES=new TypeReference<>() { };
    private final IamAccess access;
    private final TenantLiveMapper live;
    private final TenantCurrentRoleRepository roles;
    private final TenantLiveAssignmentService assignments;
    private final ResourceAccess resources;
    private final TenantLiveTransaction transactions;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;

    /** 治理范围内分页查询委派。 */
    public PageResponse<ResourceDetail<TenantDelegationRecord>> list(int page,int size,String keyword){
        var actor=access.requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_READ);IamPages.require(page,size);
        var scope=resources.objects(actor.context(),IamAction.TENANT_DELEGATION_READ);
        var visible=scope.coversAll()?null:scope.clauses().stream().flatMap(c->c.requiredIds().stream()).distinct().toList();
        String search=keyword==null?null:"%"+keyword.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return IamPages.details(live.delegations(tenant(actor),visible,search,size,IamPages.offset(page,size)).stream().map(d->detail(actor,d)).toList(),live.delegationsCount(tenant(actor),visible,search),page,size);
    }
    /** 读取可见委派，委派入口不授予独立治理资格。 */
    public ResourceDetail<TenantDelegationRecord> get(String id){var actor=access.requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_READ);resources.requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_READ,IamIds.require(id));return detail(actor,require(actor,id,false));}
    /** 原子创建完整委派。 */
    public CreatedResource create(TenantDelegationInput input){var actor=access.requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE);return transactions.execute(tx->{lock(actor,IamAction.TENANT_DELEGATION_CREATE);if(!resources.objects(actor.context(),IamAction.TENANT_DELEGATION_CREATE).coversAll())throw new BizException(IamReasonCode.ACTION_DENIED);assignments.lockRoles(actor,input.allowedRoleIds());validate(actor,input);var id=BigInteger.valueOf(access.nextId());applied(live.insertDelegation(row(actor,id,BigInteger.ZERO,input)));save(actor,id,input);audit(actor,id,null,input,BigInteger.ZERO,AuditChangeType.CREATE);changes.markAll();return new CreatedResource(id.toString(),"0");});}
    /** 原子更新限制，所有派生分配下一请求执行当前上限。 */
    public ResourceDetail<TenantDelegationRecord> replace(String id,TenantDelegationUpdateInput input){var actor=access.requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE);return transactions.execute(tx->{lock(actor,IamAction.TENANT_DELEGATION_UPDATE);assignments.lockRoles(actor,input.delegation().allowedRoleIds());resources.requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_UPDATE,IamIds.require(id));var old=require(actor,id,true);IamIds.requireVersion(input.expectedVersion(),old.version().toString());var before=input(actor,old);validate(actor,input.delegation());applied(live.updateDelegation(row(actor,old.id(),old.version(),input.delegation())));save(actor,old.id(),input.delegation());audit(actor,old.id(),before,input.delegation(),old.version().add(BigInteger.ONE),AuditChangeType.UPDATE);changes.markAll();return detail(actor,live.delegation(tenant(actor),old.id()));});}
    /** 撤销来源并保留派生记录与审计。 */
    public CreatedResource delete(String id){var actor=access.requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_DELETE);return transactions.execute(tx->{lock(actor,IamAction.TENANT_DELEGATION_DELETE);resources.requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_DELETE,IamIds.require(id));var old=require(actor,id,true);applied(live.revokeDelegation(tenant(actor),old.id(),old.version()));audit(actor,old.id(),input(actor,old),input(actor,old),old.version().add(BigInteger.ONE),AuditChangeType.REVOKE);changes.markAll();return new CreatedResource(id,old.version().add(BigInteger.ONE).toString());});}
    /** 校验拟更新委派，受影响细节按当前可见分配范围披露。 */
    public Preview<ReferenceImpactPreview> preview(String id,TenantDelegationUpdateInput input){var actor=access.requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_PREVIEW);resources.requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_PREVIEW,IamIds.require(id));var old=require(actor,id,false);IamIds.requireVersion(input.expectedVersion(),old.version().toString());validate(actor,input.delegation());var summary=new ImpactSummary(null,null,null,true);return new Preview<>(old.version().toString(),true,List.of(),List.of(),summary,new ReferenceImpactPreview(List.of(),summary));}
    /** 返回本人有效委派摘要，不依赖委派管理操作。 */
    public List<TenantDelegationMine> mine(){var actor=access.requireCurrent();if(actor.context().domain()!=AuthorizationDomain.TENANT)throw new BizException(IamReasonCode.ACTION_DENIED);var rights=access.capabilities(actor,List.of(IamAction.TENANT_ASSIGNMENT_CREATE,IamAction.TENANT_ASSIGNMENT_UPDATE));var direct=List.of(IamAction.TENANT_ASSIGNMENT_CREATE,IamAction.TENANT_ASSIGNMENT_UPDATE).stream().filter(operation->rights.allows(operation,true)).map(IamAction::getCode).toList();return live.mine(tenant(actor),new BigInteger(actor.context().memberId())).stream().map(d->{var input=input(actor,d);var allowed=input.allowedRoleIds().stream().map(id->live.role(tenant(actor),new BigInteger(id))).filter(Objects::nonNull).map(role->new CurrentRoleSummary(role.id().toString(),role.code(),role.name(),role.description(),role.groupName(),role.kind(),Boolean.TRUE.equals(role.enabled())?ConfigurationStatus.ENABLED:ConfigurationStatus.DISABLED)).toList();return new TenantDelegationMine(d.id().toString(),allowed,"指定成员与部门",instant(d.validFrom()),instant(d.validUntil()),input.maxAssignmentDuration(),issues(actor,input),direct);}).toList();}
    /** 独立治理者按实际资源参数查询范围对象。 */
    public AuthorizationCandidatePage scopeCandidates(String resourceId,String key,String keyword,List<String> ids,int page,int size){var actor=access.requireCurrent();if(!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true))throw new BizException(IamReasonCode.ACTION_DENIED);var parameter=parameter(actor,resourceId,key);return assignments.candidates(actor,parameter,null,keyword,ids,page,size,false,null);}

    private CurrentRoleParameter parameter(ActiveIdentity actor,String resourceId,String key) {
        if(live.openResource(tenant(actor),new BigInteger(resourceId))!=1)throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var supported=IamJson.read(live.resourceScopeKinds(new BigInteger(resourceId)),new TypeReference<List<ScopeKind>>() { });
        if(key.equals(TenantAuthorizationConstants.resourceParameterKey(resourceId,ScopeBindingKind.DEPARTMENTS)) && supported.contains(ScopeKind.MANAGED_DEPARTMENTS))
            return new CurrentRoleParameter(key,ScopeBindingKind.DEPARTMENTS,resourceId);
        if(key.equals(TenantAuthorizationConstants.resourceParameterKey(resourceId,ScopeBindingKind.OBJECTS)) && supported.contains(ScopeKind.OBJECT_SET))
            return new CurrentRoleParameter(key,ScopeBindingKind.OBJECTS,resourceId);
        var parameters=live.currentParameters(tenant(actor),new BigInteger(resourceId),key);
        if(parameters.size()!=1)throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var parameter=parameters.getFirst();return new CurrentRoleParameter(parameter.key(),parameter.kind(),resourceId);
    }
    private void validate(ActiveIdentity actor,TenantDelegationInput input){if(input.assignmentDurationMode()!=AssignmentDurationMode.LIMITED||input.maxAssignmentDuration()==null||input.maxAssignmentDuration().isNegative()||input.maxAssignmentDuration().isZero())invalid();
        if(live.members(tenant(actor),List.of(new BigInteger(input.administratorMemberId())))!=1)missing();
        if(input.allowedRoleIds().isEmpty()||input.allowedRoleIds().stream().distinct().count()!=input.allowedRoleIds().size()||input.resourceScopeCeilings().stream().map(ResourceScopeCeiling::resourceId).distinct().count()!=input.resourceScopeCeilings().size())invalid();
        Set<BigInteger> required=new HashSet<>();for(var id:input.allowedRoleIds()){var role=live.role(tenant(actor),new BigInteger(id));if(role==null)missing();if(!Boolean.TRUE.equals(role.enabled()))throw new BizException(IamReasonCode.ROLE_UNAVAILABLE);var def=assignments.activeDefinition(actor,role);if(!def.grants().isEmpty())live.actions(def.grants().stream().map(g->new BigInteger(g.actionId())).toList()).forEach(a->required.add(a.resourceId()));}
        var ceilings=input.resourceScopeCeilings().stream().map(c->new BigInteger(c.resourceId())).collect(java.util.stream.Collectors.toSet());if(!ceilings.containsAll(required))invalid();
        for(var c:input.resourceScopeCeilings()){if(live.resource(new BigInteger(c.resourceId()))==null)invalid();var supported=IamJson.read(live.resourceScopeKinds(new BigInteger(c.resourceId())), new TypeReference<List<ScopeKind>>() { }); List<CurrentRoleParameter> parameters=new ArrayList<>();for(var scope:c.scopes()){if(!supported.contains(scope.kind()))invalid();if(scope.parameterKey()!=null)parameters.add(new CurrentRoleParameter(scope.parameterKey(),scope.kind()==ScopeKind.MANAGED_DEPARTMENTS?ScopeBindingKind.DEPARTMENTS:ScopeBindingKind.OBJECTS,c.resourceId()));}assignments.validateBindings(actor,parameters.stream().distinct().toList(),c.scopeBindings());}
        var selection=input.recipientSelection();if(selection==null||selection.members().isEmpty()&&selection.departments().isEmpty())invalid();
        if(selection.members().contains(input.administratorMemberId()))invalid();for(var id:selection.members())if(live.members(tenant(actor),List.of(new BigInteger(id)))!=1)missing();
        for(var d:selection.departments())if(live.departmentExists(tenant(actor),new BigInteger(d.id()))!=1)missing();
    }
    private List<ScopeIssue> issues(ActiveIdentity actor,TenantDelegationInput input){var available=input.resourceScopeCeilings().stream().map(ResourceScopeCeiling::resourceId).collect(java.util.stream.Collectors.toSet());List<ScopeIssue> issues=new ArrayList<>();for(var id:input.allowedRoleIds()){var role=live.role(tenant(actor),new BigInteger(id));if(role==null)continue;var def=assignments.activeDefinition(actor,role);if(def.grants().isEmpty())continue;for(var a:live.actions(def.grants().stream().map(g->new BigInteger(g.actionId())).toList()))if(!available.contains(a.resourceId().toString()))issues.add(new ScopeIssue(a.resourceId().toString(),List.of(a.actionId().toString()),null,ScopeIssueReason.MISSING_RESOURCE_CEILING));}return issues;}
    private TenantDelegationInput input(ActiveIdentity actor,TenantLiveRows.Delegation d){return new TenantDelegationInput(d.administratorMemberId().toString(),live.delegatedRoles(d.id()).stream().map(BigInteger::toString).toList(),new Selection(live.recipientMembers(tenant(actor),d.id()).stream().map(BigInteger::toString).toList(),live.recipientDepartments(tenant(actor),d.id())),live.ceilings(d.id()).stream().map(c->new ResourceScopeCeiling(c.resourceId().toString(),IamJson.read(c.scopes(),SCOPES),IamJson.read(c.scopeBindings(),BINDINGS))).toList(),instant(d.validFrom()),instant(d.validUntil()),Duration.ofSeconds(d.durationSeconds().longValueExact(),d.durationNanos()),AssignmentDurationMode.LIMITED);}
    private ResourceDetail<TenantDelegationRecord> detail(ActiveIdentity actor,TenantLiveRows.Delegation d){var input=input(actor,d);var capabilities=new LinkedHashMap<String,ObjectCapability>();
        var allowed=access.capabilities(actor,List.of(IamAction.TENANT_DELEGATION_READ,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_DELEGATION_DELETE,IamAction.TENANT_DELEGATION_PREVIEW));
        for(var operation:List.of(IamAction.TENANT_DELEGATION_READ,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_DELEGATION_DELETE,IamAction.TENANT_DELEGATION_PREVIEW)) {
            boolean grant=allowed.allows(operation,true) && com.ingot.cloud.iam.persistence.ObjectScopeSql.matches(resources.objects(actor.context(),operation),d.id().longValueExact());
            if(operation==IamAction.TENANT_DELEGATION_UPDATE&&d.status()!=GrantStatus.ACTIVE)grant=false;
            capabilities.put(operation.getCode(),new ObjectCapability(grant,grant?null:IamReasonCode.ACTION_DENIED,grant?null:"当前对象不允许此操作"));
        }
        return IamDetails.of(new TenantDelegationRecord(d.id().toString(),input,d.status(),d.administratorName(),issues(actor,input)),capabilities,d.version().toString());}
    private void save(ActiveIdentity actor,BigInteger id,TenantDelegationInput input){live.clearAllowedRoles(id);live.clearCeilings(id);live.clearRecipientMembers(tenant(actor),id);live.clearRecipientDepartments(tenant(actor),id);
        for(var roleId:input.allowedRoleIds()){var role=live.role(tenant(actor),new BigInteger(roleId));live.allowedRole(id,tenant(actor),role,role.tenantId()==null?BigInteger.ZERO:tenant(actor));}
        for(var c:input.resourceScopeCeilings())live.ceiling(id,new BigInteger(c.resourceId()),IamJson.array(c.scopes()),IamJson.object(c.scopeBindings()));
        for(var member:input.recipientSelection().members())live.recipientMember(BigInteger.valueOf(access.nextId()),tenant(actor),id,new BigInteger(member));for(var d:input.recipientSelection().departments())live.recipientDepartment(tenant(actor),id,new BigInteger(d.id()),Boolean.TRUE.equals(d.includeDescendants()));}
    private TenantLiveRows.Delegation row(ActiveIdentity actor,BigInteger id,BigInteger version,TenantDelegationInput input){return new TenantLiveRows.Delegation(id,tenant(actor),new BigInteger(input.administratorMemberId()),time(input.validFrom()),time(input.validUntil()),BigInteger.valueOf(input.maxAssignmentDuration().getSeconds()),input.maxAssignmentDuration().getNano(),GrantStatus.ACTIVE,version,null);}
    private TenantLiveRows.Delegation require(ActiveIdentity actor,String id,boolean lock){var d=lock?live.lockDelegation(tenant(actor),new BigInteger(id)):live.delegation(tenant(actor),new BigInteger(id));if(d==null)missing();return d;}
    private void lock(ActiveIdentity actor,IamAction action){if(live.lockTenant(tenant(actor))==null)missing();access.requireGoverned(AuthorizationDomain.TENANT,action);}
    private void audit(ActiveIdentity actor,BigInteger id,TenantDelegationInput before,TenantDelegationInput after,BigInteger version,AuditChangeType type) {
        audits.write(actor.context(),access.nextId(),TARGET,id.toString(),type,before==null?Map.of():safe(before,GrantStatus.ACTIVE),safe(after,type==AuditChangeType.REVOKE?GrantStatus.REVOKED:GrantStatus.ACTIVE),Map.of(TARGET,version.toString()),id.toString(),null);
    }
    private Map<AuditField,Object> safe(TenantDelegationInput input,GrantStatus status) {
        var result=new LinkedHashMap<AuditField,Object>();
        result.put(AuditField.SCOPE,new AuditScope(input.administratorMemberId(),input.allowedRoleIds(),input.resourceScopeCeilings(),input.maxAssignmentDuration().toString()));
        result.put(AuditField.RECIPIENT_SELECTION,input.recipientSelection());result.put(AuditField.STATUS,status.getValue());
        if(input.validFrom()!=null)result.put(AuditField.VALID_FROM,input.validFrom().toString());if(input.validUntil()!=null)result.put(AuditField.VALID_UNTIL,input.validUntil().toString());
        return result;
    }
    /** 不含联系方式或业务值的委派授权事实。 */
    private record AuditScope(String administratorMemberId,List<String> allowedRoleIds,List<ResourceScopeCeiling> resourceScopeCeilings,String maxAssignmentDuration) { }

    private static BigInteger tenant(ActiveIdentity actor){return new BigInteger(actor.context().tenantId());}
    private static LocalDateTime time(Instant i){return i==null?null:LocalDateTime.ofInstant(i,ZoneOffset.UTC);}
    private static Instant instant(LocalDateTime i){return i==null?null:i.toInstant(ZoneOffset.UTC);}
    private static void invalid(){throw new BizException(IamReasonCode.INVALID_ARGUMENT);}
    private static void missing(){throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);}
    private static void applied(int n){if(n!=1)throw new BizException(IamReasonCode.REVISION_CONFLICT);}
}
