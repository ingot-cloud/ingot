package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.mapper.*;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.role.TenantCurrentRoleService;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>租户当前角色分配与委派准入；所有写入事务内重验，平台分配继续使用原服务。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantLiveAssignmentService {
    private static final String TARGET = "assignment";
    private static final String SCOPE_FIELD = "scopeBindings";
    private static final TypeReference<Map<String,ScopeBinding>> BINDINGS = new TypeReference<>() { };
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() { };
    private final IamAccess access;
    private final TenantLiveMapper live;
    private final TenantCurrentRoleRepository roles;
    private final TenantCurrentRoleService roleService;
    private final ResourceAccess resources;
    private final TenantLiveTransaction transactions;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final TenantScopeCandidateMapper candidates;
    private final DepartmentClosure departments;

    /** 分页读取允许披露的当前分配。 */
    public PageResponse<ResourceDetail<TenantAssignmentRecord>> list(int page,int size,SubjectType type,String keyword) {
        var admission=access.admit(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ);
        var actor=admission.actor(); IamPages.require(page,size);
        List<BigInteger> visible=admission.governed()?visible(actor,IamAction.TENANT_ASSIGNMENT_READ):null;
        List<BigInteger> sources=admission.governed()?null:live.mine(tenant(actor),member(actor)).stream().map(TenantLiveRows.Delegation::id).toList();
        var items=live.assignments(tenant(actor),visible,sources,type,search(keyword),size,IamPages.offset(page,size)).stream().map(row->detail(actor,row)).toList();
        return IamPages.details(items,live.assignmentsCount(tenant(actor),visible,sources,type,search(keyword)),page,size);
    }

    /** 原子提交批次，受限管理员只能使用同一条本人有效委派。 */
    public CreatedResource create(TenantAssignmentBatchInput batch) {
        var actor=access.require(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE);
        return transactions.execute(tx->{ lock(actor,IamAction.TENANT_ASSIGNMENT_CREATE); lockRoles(actor,batch.items().stream().map(TenantAssignmentInput::roleId).toList()); validateBatchSource(actor,batch,IamAction.TENANT_ASSIGNMENT_CREATE);
            long first=0; for(var input:batch.items()) {
                validate(actor,input,IamAction.TENANT_ASSIGNMENT_CREATE); long id=access.nextId();if(first==0)first=id;
                var role=role(actor,input.roleId()); var row=row(actor,id,BigInteger.ZERO,input,role);
                applied(live.insertAssignment(row,role.tenantId()==null?BigInteger.ZERO:tenant(actor)));
                audit(actor,null,row,AuditChangeType.CREATE); }
            changes.markAll();return new CreatedResource(Long.toString(first),"0");});
    }

    /** 预览使用与提交相同的当前角色、范围和来源校验。 */
    public Preview<AssignmentPreviewResult> preview(TenantAssignmentBatchInput batch) {
        var operation=access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,false)?IamAction.TENANT_ASSIGNMENT_CREATE:IamAction.TENANT_ASSIGNMENT_UPDATE;
        var actor=access.require(AuthorizationDomain.TENANT,operation);
        validateBatchSource(actor,batch,operation);List<AssignmentPreviewItem> items=new ArrayList<>();List<ValidationIssue> errors=new ArrayList<>();
        for(var input:batch.items()) {try {validate(actor,input,operation);
            items.add(new AssignmentPreviewItem(input.subject(),true,List.of(),roles.definition(role(actor,input.roleId())).grants()));
        }catch(BizException ex){var reason=Arrays.stream(IamReasonCode.values()).filter(e->e.getCode().equals(ex.getCode())).findFirst().orElse(IamReasonCode.INVALID_ARGUMENT);
            if(reason==IamReasonCode.AUTHORIZATION_UNAVAILABLE)throw ex;var issue=new ValidationIssue(SCOPE_FIELD,reason,reason.getText());errors.add(issue);
            items.add(new AssignmentPreviewItem(input.subject(),false,List.of(issue),List.of()));}}
        return new Preview<>(actor.memberVersion(),errors.isEmpty(),errors,List.of(),new ImpactSummary((long)items.size(),(long)items.size(),0L,false),new AssignmentPreviewResult(items));
    }

    /** 调整完整当前分配，来源不能由受限管理员替换。 */
    public ResourceDetail<TenantAssignmentRecord> replace(String id,TenantAssignmentUpdateInput input) {
        var actor=access.require(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE);
        return transactions.execute(tx->{lock(actor,IamAction.TENANT_ASSIGNMENT_UPDATE);lockRoles(actor,List.of(input.assignment().roleId()));var old=require(actor,id,true);
            authorizeExisting(actor,old,IamAction.TENANT_ASSIGNMENT_UPDATE);IamIds.requireVersion(input.expectedVersion(),old.version().toString());
            if(!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,true)
                && !Objects.equals(old.delegationGrantId()==null?null:old.delegationGrantId().toString(),input.assignment().delegationGrantId()))denied();
            validate(actor,input.assignment(),IamAction.TENANT_ASSIGNMENT_UPDATE);var role=role(actor,input.assignment().roleId());
            var next=row(actor,old.id().longValueExact(),old.version(),input.assignment(),role);
            applied(live.updateAssignment(next,role.tenantId()==null?BigInteger.ZERO:tenant(actor)));audit(actor,old,next,AuditChangeType.UPDATE);changes.markAll();
            return detail(actor,live.assignment(tenant(actor),old.id()));});
    }

    /** 撤销分配并保留历史。 */
    public CreatedResource delete(String id) {
        var actor=access.require(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_DELETE);
        return transactions.execute(tx->{lock(actor,IamAction.TENANT_ASSIGNMENT_DELETE);var old=require(actor,id,true);
            authorizeExisting(actor,old,IamAction.TENANT_ASSIGNMENT_DELETE);applied(live.revokeAssignment(tenant(actor),old.id(),old.version()));
            audit(actor,old,old,AuditChangeType.REVOKE);changes.markAll();return new CreatedResource(id,old.version().add(BigInteger.ONE).toString());});
    }

    /** 查询当前可选角色、成员、组和资源，已选ID使用同一来源过滤。 */
    public PageResponse<TenantAssignmentOption> options(TenantAssignmentOptionKind kind,String source,String keyword,List<String> ids,int page,int size) {
        var actor=optionActor();candidateAdmission(actor,kind);var grant=source(actor,source,null);if(grant!=null&&(kind==TenantAssignmentOptionKind.APPLICATION||kind==TenantAssignmentOptionKind.DEPARTMENT))denied();IamPages.require(page,size);
        var query=new TenantLiveOptionSql.Query(tenant(actor),member(actor),grant==null?null:grant.id(),kind,search(keyword),ids(ids),size,IamPages.offset(page,size));
        return new PageResponse<>(live.options(query).stream().map(o->new TenantAssignmentOption(o.id().toString(),o.name(),kind,o.roleKind())).toList(),live.optionsCount(query),page,size);
    }

    /** 受限分配专用角色详情，不补通用角色读取资格。 */
    public CurrentRoleGrantList grants(String id,String source) {
        var actor=optionActor();candidateAdmission(actor,TenantAssignmentOptionKind.ROLE);var grant=source(actor,source,null);var role=role(actor,id);
        if(grant!=null&&!live.delegatedRoles(grant.id()).contains(role.id()))denied();
        var opened=live.applications(tenant(actor)).stream().map(TenantLiveRows.Application::id).collect(java.util.stream.Collectors.toSet());
        var list=roleService.grantsForAssignment(actor,id);
        var actions=list.items().stream().filter(CurrentRoleCatalogAction::enabled).filter(a->opened.contains(new BigInteger(a.applicationId()))).toList();
        var allowed=actions.stream().map(CurrentRoleCatalogAction::actionId).collect(java.util.stream.Collectors.toSet());
        var grants=list.definition().grants().stream().filter(g->allowed.contains(g.actionId())).toList();
        var used=grants.stream().flatMap(g->g.scopes().stream()).map(ScopeExpression::parameterKey).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        return new CurrentRoleGrantList(actions,new CurrentRoleDefinition(grants,list.definition().parameterDefinitions().stream().filter(p->used.contains(p.key())).toList()));
    }

    /** 当前角色参数候选；未知业务资源返回明确不支持。 */
    public AuthorizationCandidatePage scopeCandidates(String roleId,String key,String source,String keyword,List<String> selected,int page,int size,boolean tree,String parent) {
        var actor=optionActor();var grant=source(actor,source,null);var role=role(actor,roleId);
        if(grant!=null&&!live.delegatedRoles(grant.id()).contains(role.id()))denied();
        var definition=roles.definition(role);var parameter=definition.parameterDefinitions().stream().filter(p->p.key().equals(key)).findFirst().orElseThrow(()->new BizException(IamReasonCode.INVALID_ARGUMENT));
        return candidates(actor,parameter,grant,keyword,selected,page,size,tree,parent);
    }

    /** 按真实资源及参数查询范围候选，供独立委派治理复用。 */
    public AuthorizationCandidatePage candidates(ActiveIdentity actor,CurrentRoleParameter parameter,TenantLiveRows.Delegation source,
        String keyword,List<String> selected,int page,int size,boolean tree,String parent) {
        IamPages.require(page,size);var target=target(parameter);
        if(target==null)return new AuthorizationCandidatePage(List.of(),0,page,size,false,"该资源暂未接入范围对象查询");
        List<BigInteger> allowed=source==null?null:allowedBindings(actor,parameter,source);
        var chosen=ids(selected);if(allowed!=null){if(chosen.isEmpty())chosen=allowed;else chosen=chosen.stream().filter(allowed::contains).toList();
            if(chosen.isEmpty())return new AuthorizationCandidatePage(List.of(),0,page,size,true,null);}
        var q=new TenantScopeCandidateSql.Query(target,tenant(actor).longValueExact(),search(keyword),chosen,IamPages.offset(page,size),size,tree&&allowed==null,parent==null?null:new BigInteger(parent));
        var rows=candidates.page(q);return new AuthorizationCandidatePage(rows.stream().map(o->new AuthorizationOption(o.id().toString(),o.name(),null,null,null,null,null,null,
            o.parentId()==null?null:o.parentId().toString(),o.hasChildren(),null)).toList(),candidates.count(q),page,size,true,null);
    }

    private List<BigInteger> allowedBindings(ActiveIdentity actor,CurrentRoleParameter parameter,TenantLiveRows.Delegation source) {
        var limit=live.ceilings(source.id()).stream().filter(c->c.resourceId().toString().equals(parameter.resourceId())).findFirst().orElse(null);
        if(limit==null)return List.of();
        var clauses=ScopeBinder.bind(new ActionScopeCeiling(parameter.resourceId(),IamJson.read(limit.scopes(),SCOPES),IamJson.read(limit.scopeBindings(),BINDINGS)));
        if(clauses.stream().anyMatch(ScopeClause::all))return null;
        var target=target(parameter);
        Set<BigInteger> union=new LinkedHashSet<>();
        List<BigInteger> recipients=null;
        for(var clause:clauses) {
            List<Set<BigInteger>> restrictions=new ArrayList<>();
            if(!clause.objectIds().isEmpty()) {
                var objects=clause.objectIds().stream().map(BigInteger::new).toList();
                restrictions.add(new LinkedHashSet<>(parameter.kind()==ScopeBindingKind.DEPARTMENTS && target!=TenantScopeObjectResource.DEPARTMENT
                    ? live.memberDepartments(tenant(actor),objects) : objects));
            }
            if(!clause.departmentIds().isEmpty()) {
                var deptIds=departments.expand(tenant(actor),clause.departmentIds().stream().map(BigInteger::new).toList(),clause.departmentDescendants());
                restrictions.add(new LinkedHashSet<>(parameter.kind()==ScopeBindingKind.DEPARTMENTS || target==TenantScopeObjectResource.DEPARTMENT
                    ? deptIds : (deptIds.isEmpty()?List.<BigInteger>of():live.membersInDepartments(tenant(actor),List.copyOf(deptIds)))));
            }
            if(clause.self()||clause.memberDepartments()) {
                if(recipients==null)recipients=live.scopeRecipients(new TenantLiveOptionSql.Query(tenant(actor),source.administratorMemberId(),source.id(),TenantAssignmentOptionKind.MEMBER,"%",List.of(),IamPages.DEFAULT_SIZE,0));
                var own=recipients.isEmpty()?List.<BigInteger>of():live.memberDepartments(tenant(actor),recipients);
                if(clause.self())restrictions.add(new LinkedHashSet<>(parameter.kind()==ScopeBindingKind.DEPARTMENTS || target==TenantScopeObjectResource.DEPARTMENT ? own : recipients));
                if(clause.memberDepartments()) {
                    var deptIds=departments.expand(tenant(actor),own,clause.memberDepartmentDescendants());
                    restrictions.add(new LinkedHashSet<>(parameter.kind()==ScopeBindingKind.DEPARTMENTS || target==TenantScopeObjectResource.DEPARTMENT ? deptIds : (deptIds.isEmpty()?List.<BigInteger>of():live.membersInDepartments(tenant(actor),List.copyOf(deptIds)))));
                }
            }
            if(!restrictions.isEmpty()) {
                var intersection=restrictions.getFirst();
                restrictions.stream().skip(1).forEach(intersection::retainAll);
                union.addAll(intersection);
            }
        }
        return List.copyOf(union);
    }

    /** 校验当前完整绑定及持续委派约束，绝不自动扩大参数。 */
    public void validate(ActiveIdentity actor,TenantAssignmentInput input,IamAction action) {
        var role=role(actor,input.roleId()); if(!Boolean.TRUE.equals(role.enabled()))throw new BizException(IamReasonCode.ROLE_UNAVAILABLE); var definition=activeDefinition(actor,role);
        var grant=source(actor,input.delegationGrantId(),action);
        if(grant==null&&action==IamAction.TENANT_ASSIGNMENT_CREATE&&!resources.objects(actor.context(),action).coversAll())denied();
        if(grant!=null&&!live.delegatedRoles(grant.id()).contains(role.id()))exceeded();
        if(input.subject().type()==SubjectType.MEMBER){if(live.members(tenant(actor),List.of(new BigInteger(input.subject().id())))!=1)missing();}
        else if(live.groupExists(tenant(actor),new BigInteger(input.subject().id()))!=1)missing();
        validateBindings(actor,definition.parameterDefinitions(),input.scopeBindings());
        if(grant!=null){var recipients=input.subject().type()==SubjectType.MEMBER?List.of(new BigInteger(input.subject().id())):live.groupMembers(tenant(actor),new BigInteger(input.subject().id()));
            if(input.subject().type()==SubjectType.GROUP
                ? !live.groupWithinSource(new TenantLiveOptionSql.Query(tenant(actor),grant.administratorMemberId(),grant.id(),TenantAssignmentOptionKind.GROUP,"%",List.of(),1,0),new BigInteger(input.subject().id()))
                : recipients.isEmpty()||recipients.stream().anyMatch(id->id.equals(member(actor))||live.recipient(tenant(actor),grant.id(),id)==0))exceeded();
            Instant from=input.validFrom()==null?Instant.now():input.validFrom(),until=input.validUntil();
            if(until==null||!until.isAfter(from)||Duration.between(from,until).compareTo(Duration.ofSeconds(grant.durationSeconds().longValueExact(),grant.durationNanos()))>0
                ||grant.validFrom()!=null&&from.isBefore(instant(grant.validFrom()))||grant.validUntil()!=null&&until.isAfter(instant(grant.validUntil())))exceeded();
            var limits=live.ceilings(grant.id());var actions=definition.grants().isEmpty()?List.<TenantLiveRows.Action>of():live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList());
            for(var g:definition.grants()){var a=actions.stream().filter(v->v.actionId().toString().equals(g.actionId())).findFirst().orElseThrow(()->new BizException(IamReasonCode.INVALID_ARGUMENT));
                var limit=limits.stream().filter(c->c.resourceId().equals(a.resourceId())).findFirst().orElse(null);if(limit==null)exceeded();
                // 固定角色ALL/SELF声明由资源上限持续收缩；仅拒绝操作者显式配置的越界绑定。
                for(var expression:g.scopes()) {
                    if(expression.parameterKey()==null)continue;
                    var parameter=definition.parameterDefinitions().stream().filter(p->p.key().equals(expression.parameterKey())).findFirst().orElseThrow();
                    var allowed=allowedBindings(actor,parameter,grant);
                    if(allowed!=null && input.scopeBindings().get(parameter.key()).ids().stream().map(BigInteger::new).anyMatch(id->!allowed.contains(id)))exceeded();
                }}
        }
    }

    /** 按资源真实归属校验全部命名绑定。 */
    public void validateBindings(ActiveIdentity actor,List<CurrentRoleParameter> parameters,Map<String,ScopeBinding> bindings) {
        if(bindings==null||bindings.size()!=parameters.size())invalid();
        for(var p:parameters){var b=bindings.get(p.key());if(b==null||b.kind()!=p.kind()||b.ids()==null)invalid();
            var target=target(p);if(target==null&&!b.ids().isEmpty())invalid();if(b.ids().isEmpty())continue;
            var values=ids(b.ids());var q=new TenantScopeCandidateSql.Query(target,tenant(actor).longValueExact(),"%",values,0,IamPages.MAX_SIZE);
            if(candidates.count(q)!=values.size())invalid();}
    }

    /** 本人有效来源；独立资格可不选来源。 */
    public TenantLiveRows.Delegation source(ActiveIdentity actor,String id,IamAction action) {
        boolean governed=action==null?(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_PREVIEW,true))
            :access.allows(AuthorizationDomain.TENANT,action,true);
        if(id==null||id.isBlank()){if(!governed)denied();return null;}
        var source=live.mine(tenant(actor),member(actor)).stream().filter(d->d.id().toString().equals(id)).findFirst().orElseThrow(()->new BizException(IamReasonCode.DELEGATION_EXCEEDED));
        return source;
    }

    /** 计算当前定义待补参数或资源上限，旧有效资源持续可用。 */
    public List<ScopeIssue> issues(ActiveIdentity actor,TenantLiveRows.Role role,Map<String,ScopeBinding> bindings,BigInteger source) {
        var definition=activeDefinition(actor,role);List<ScopeIssue> issues=new ArrayList<>();
        for(var p:definition.parameterDefinitions()){var b=bindings.get(p.key());if(b==null||b.kind()!=p.kind())issues.add(new ScopeIssue(p.resourceId(),
            definition.grants().stream().filter(g->g.scopes().stream().anyMatch(s->p.key().equals(s.parameterKey()))).map(ActionGrant::actionId).toList(),p.key(),b==null?ScopeIssueReason.MISSING_BINDING:ScopeIssueReason.INVALID_BINDING));}
        if(source!=null&&!definition.grants().isEmpty()){var limits=live.ceilings(source).stream().map(TenantLiveRows.Ceiling::resourceId).collect(java.util.stream.Collectors.toSet());
            for(var a:live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList()))if(!limits.contains(a.resourceId()))issues.add(new ScopeIssue(a.resourceId().toString(),List.of(a.actionId().toString()),null,ScopeIssueReason.MISSING_RESOURCE_CEILING));}
        return List.copyOf(issues);
    }

    /** 分配仅配置本组织当前可执行资源；未开通或停用资源不制造不可见必填参数。 */
    public CurrentRoleDefinition activeDefinition(ActiveIdentity actor,TenantLiveRows.Role role) {
        var definition=roles.definition(role);
        if(definition.grants().isEmpty())return definition;
        var opened=live.applications(tenant(actor)).stream().map(TenantLiveRows.Application::id).collect(java.util.stream.Collectors.toSet());
        var ids=live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList()).stream()
            .filter(a->Boolean.TRUE.equals(a.enabled()) && opened.contains(a.applicationId())).map(a->a.actionId().toString()).collect(java.util.stream.Collectors.toSet());
        var grants=definition.grants().stream().filter(g->ids.contains(g.actionId())).toList();
        var keys=grants.stream().flatMap(g->g.scopes().stream()).map(ScopeExpression::parameterKey).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        return new CurrentRoleDefinition(grants,definition.parameterDefinitions().stream().filter(p->keys.contains(p.key())).toList());
    }

    private ResourceDetail<TenantAssignmentRecord> detail(ActiveIdentity actor,TenantLiveRows.Assignment row) {
        var input=new TenantAssignmentInput(new SubjectRef(row.subjectType(),(row.subjectType()==SubjectType.MEMBER?row.memberId():row.groupId()).toString()),row.roleId().toString(),IamJson.read(row.scopeBindings(),BINDINGS),instant(row.validFrom()),instant(row.validUntil()),row.delegationGrantId()==null?null:row.delegationGrantId().toString());
        var role=role(actor,row.roleId().toString());var state=row.status()==GrantStatus.REVOKED?AssignmentEffectiveStatus.REVOKED:
            row.validFrom().isAfter(LocalDateTime.now(ZoneOffset.UTC))?AssignmentEffectiveStatus.PENDING:row.validUntil()!=null&&!row.validUntil().isAfter(LocalDateTime.now(ZoneOffset.UTC))?AssignmentEffectiveStatus.EXPIRED:AssignmentEffectiveStatus.ACTIVE;
        if((state==AssignmentEffectiveStatus.ACTIVE||state==AssignmentEffectiveStatus.PENDING)&&(!Boolean.TRUE.equals(role.enabled())||row.delegationGrantId()!=null&&sourceInvalid(actor,row)))state=AssignmentEffectiveStatus.SOURCE_INVALID;
        var record=new TenantAssignmentRecord(row.id().toString(),input,row.status(),row.source(),row.subjectName(),row.roleName(),row.roleKind(),row.delegationGrantId()==null?null:row.delegationGrantId().toString(),instant(row.createdAt()),null,state,issues(actor,role,input.scopeBindings(),row.delegationGrantId()));
        Map<String,ObjectCapability> capabilities=new LinkedHashMap<>();
        var available=access.capabilities(actor,List.of(IamAction.TENANT_ASSIGNMENT_UPDATE,IamAction.TENANT_ASSIGNMENT_DELETE));
        var ownSources=live.mine(tenant(actor),member(actor)).stream().map(TenantLiveRows.Delegation::id).collect(java.util.stream.Collectors.toSet());
        for(var operation:List.of(IamAction.TENANT_ASSIGNMENT_UPDATE,IamAction.TENANT_ASSIGNMENT_DELETE)) {
            boolean allowed=available.allows(operation,false);
            if(allowed) allowed=available.allows(operation,true)
                ? ObjectScopeSql.matches(resources.objects(actor.context(),operation),row.id().longValueExact())
                : row.delegationGrantId()!=null && ownSources.contains(row.delegationGrantId());
            if(operation==IamAction.TENANT_ASSIGNMENT_UPDATE && row.status()!=GrantStatus.ACTIVE)allowed=false;
            capabilities.put(operation.getCode(),new ObjectCapability(allowed,allowed?null:IamReasonCode.ACTION_DENIED,allowed?null:"当前来源或对象范围不允许"));
        }
        return IamDetails.of(record,capabilities,row.version().toString());
    }
    private boolean sourceInvalid(ActiveIdentity actor,TenantLiveRows.Assignment row) {
        var source=live.delegation(tenant(actor),row.delegationGrantId());var now=LocalDateTime.now(ZoneOffset.UTC);
        if(source==null||source.status()!=GrantStatus.ACTIVE||!live.delegatedRoles(source.id()).contains(row.roleId())
            ||source.validFrom()!=null&&source.validFrom().isAfter(now)||source.validUntil()!=null&&!source.validUntil().isAfter(now)
            ||live.members(tenant(actor),List.of(source.administratorMemberId()))!=1||row.validUntil()==null
            ||source.validFrom()!=null&&row.validFrom().isBefore(source.validFrom())||source.validUntil()!=null&&row.validUntil().isAfter(source.validUntil())
            ||Duration.between(row.validFrom(),row.validUntil()).compareTo(Duration.ofSeconds(source.durationSeconds().longValueExact(),source.durationNanos()))>0)return true;
        return row.subjectType()==SubjectType.GROUP
            ? !live.groupWithinSource(new TenantLiveOptionSql.Query(tenant(actor),source.administratorMemberId(),source.id(),TenantAssignmentOptionKind.GROUP,"%",List.of(),1,0),row.groupId())
            : live.recipient(tenant(actor),source.id(),row.memberId())==0;
    }
    private TenantLiveRows.Assignment require(ActiveIdentity actor,String id,boolean lock){var row=lock?live.lockAssignment(tenant(actor),new BigInteger(id)):live.assignment(tenant(actor),new BigInteger(id));if(row==null)missing();return row;}
    private void authorizeExisting(ActiveIdentity actor,TenantLiveRows.Assignment row,IamAction action){if(access.allows(AuthorizationDomain.TENANT,action,true))resources.requireVisibleObject(actor.context(),action,row.id().longValueExact());else source(actor,row.delegationGrantId()==null?null:row.delegationGrantId().toString(),action);}
    private void validateBatchSource(ActiveIdentity actor,TenantAssignmentBatchInput batch,IamAction operation){if(batch.items()==null||batch.items().isEmpty())invalid();if(!access.allows(AuthorizationDomain.TENANT,operation,true)&&batch.items().stream().map(TenantAssignmentInput::delegationGrantId).distinct().count()!=1)exceeded();}
    private void candidateAdmission(ActiveIdentity actor,TenantAssignmentOptionKind kind) {
        var rights=access.capabilities(actor,List.of(IamAction.TENANT_ASSIGNMENT_READ,IamAction.TENANT_ASSIGNMENT_CREATE,IamAction.TENANT_ASSIGNMENT_UPDATE,IamAction.TENANT_DELEGATION_CREATE,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_ROLE_CREATE,IamAction.TENANT_ROLE_UPDATE,IamAction.TENANT_ROLE_PREVIEW));
        boolean delegation=rights.allows(IamAction.TENANT_DELEGATION_CREATE,true)||rights.allows(IamAction.TENANT_DELEGATION_UPDATE,true);
        boolean assignment=rights.allows(IamAction.TENANT_ASSIGNMENT_READ,false)||rights.allows(IamAction.TENANT_ASSIGNMENT_CREATE,false)||rights.allows(IamAction.TENANT_ASSIGNMENT_UPDATE,false);
        boolean role=rights.allows(IamAction.TENANT_ROLE_CREATE,true)||rights.allows(IamAction.TENANT_ROLE_UPDATE,true)||rights.allows(IamAction.TENANT_ROLE_PREVIEW,true);
        if(kind==TenantAssignmentOptionKind.DEPARTMENT ? !delegation : !(assignment||delegation||role&&(kind==TenantAssignmentOptionKind.APPLICATION||kind==TenantAssignmentOptionKind.RESOURCE)))denied();
    }
    private ActiveIdentity optionActor(){var actor=access.requireCurrent();if(actor.context().domain()!=AuthorizationDomain.TENANT)denied();if(!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ,false)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,false)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,false)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_CREATE,true)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_UPDATE,true)&&!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_PREVIEW,true))denied();return actor;}
    /** 锁定批次角色头，防止校验期间共享权限发生切换；统一按角色ID排序。 */
    public void lockRoles(ActiveIdentity actor,List<String> roleIds) {
        roleIds.stream().map(BigInteger::new).distinct().sorted().forEach(id->{
            var row=live.lockRole(id);
            if(row==null || row.kind()!=RoleKind.SHARED && (row.kind()!=RoleKind.TENANT_CUSTOM || !tenant(actor).equals(row.tenantId())))missing();
        });
    }
    private TenantLiveRows.Role role(ActiveIdentity actor,String id){
        var role=org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
            ? live.lockRole(new BigInteger(id)) : live.role(tenant(actor),new BigInteger(id));
        if(role==null || role.kind()!=RoleKind.SHARED && (role.kind()!=RoleKind.TENANT_CUSTOM || !tenant(actor).equals(role.tenantId())))missing();return role;
    }
    private void lock(ActiveIdentity actor,IamAction action){if(live.lockTenant(tenant(actor))==null)missing();access.require(AuthorizationDomain.TENANT,action);}
    private List<BigInteger> visible(ActiveIdentity actor,IamAction action){var scope=resources.objects(actor.context(),action);return scope.coversAll()?null:scope.clauses().stream().flatMap(c->c.requiredIds().stream()).distinct().toList();}
    private TenantScopeObjectResource target(CurrentRoleParameter p){if(p.kind()==ScopeBindingKind.DEPARTMENTS)return TenantScopeObjectResource.DEPARTMENT;var row=live.resource(new BigInteger(p.resourceId()));return row==null||!TenantAuthorizationConstants.CORE_APPLICATION.equals(row.applicationCode())?null:TenantScopeObjectResource.find(row.resourceCode());}
    private TenantLiveRows.Assignment row(ActiveIdentity actor,long id,BigInteger version,TenantAssignmentInput input,TenantLiveRows.Role role){return new TenantLiveRows.Assignment(BigInteger.valueOf(id),tenant(actor),input.subject().type(),input.subject().type()==SubjectType.MEMBER?new BigInteger(input.subject().id()):null,input.subject().type()==SubjectType.GROUP?new BigInteger(input.subject().id()):null,role.id(),role.kind(),role.currentRevisionId(),IamJson.object(input.scopeBindings()),input.delegationGrantId()==null?null:new BigInteger(input.delegationGrantId()),time(input.validFrom()==null?Instant.now():input.validFrom()),time(input.validUntil()),GrantStatus.ACTIVE,AssignmentSource.MANUAL,version,null,null,null);}
    private void audit(ActiveIdentity actor,TenantLiveRows.Assignment before,TenantLiveRows.Assignment after,AuditChangeType type) {
        audits.write(actor.context(),access.nextId(),TARGET,after.id().toString(),type,before==null?Map.of():safe(before,before.status()),safe(after,type==AuditChangeType.REVOKE?GrantStatus.REVOKED:after.status()),Map.of(TARGET,after.version().add(type==AuditChangeType.CREATE?BigInteger.ZERO:BigInteger.ONE).toString()),after.delegationGrantId()==null?null:after.delegationGrantId().toString(),after.id().toString());
    }
    private Map<AuditField,Object> safe(TenantLiveRows.Assignment row,GrantStatus status) {
        var result=new LinkedHashMap<AuditField,Object>();
        result.put(AuditField.ROLE,row.roleId().toString());result.put(AuditField.ROLE_REVISION,row.currentRevisionId().toString());
        result.put(AuditField.RECIPIENT_SELECTION,new SubjectRef(row.subjectType(),(row.subjectType()==SubjectType.MEMBER?row.memberId():row.groupId()).toString()));
        result.put(AuditField.SCOPE,IamJson.read(row.scopeBindings(),BINDINGS));result.put(AuditField.STATUS,status.getValue());
        result.put(AuditField.VALID_FROM,instant(row.validFrom()).toString());if(row.validUntil()!=null)result.put(AuditField.VALID_UNTIL,instant(row.validUntil()).toString());
        return result;
    }
    private static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
    private static BigInteger member(ActiveIdentity a){return new BigInteger(a.context().memberId());}
    private static List<BigInteger> ids(List<String> values){if(values==null)return List.of();if(values.size()>IamPages.MAX_SIZE)invalid();return values.stream().map(v->BigInteger.valueOf(IamIds.require(v))).distinct().toList();}
    private static String search(String v){return "%"+(v==null?"":v.trim()).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private static LocalDateTime time(Instant i){return i==null?null:LocalDateTime.ofInstant(i,ZoneOffset.UTC);}
    private static Instant instant(LocalDateTime i){return i==null?null:i.toInstant(ZoneOffset.UTC);}
    private static void applied(int count){if(count!=1)throw new BizException(IamReasonCode.REVISION_CONFLICT);}
    private static void invalid(){throw new BizException(IamReasonCode.INVALID_ARGUMENT);}
    private static void missing(){throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);}
    private static void denied(){throw new BizException(IamReasonCode.ACTION_DENIED);}
    private static void exceeded(){throw new BizException(IamReasonCode.DELEGATION_EXCEEDED);}
}
