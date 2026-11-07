package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.persistence.MemberPickerBranch;
import com.ingot.cloud.iam.infrastructure.persistence.TenantAuthorizationOptionSql;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceSql;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.cloud.iam.application.assignment.model.AssignmentPreviewItem;
import com.ingot.cloud.iam.application.assignment.model.AssignmentPreviewResult;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentOption;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentRecord;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.CurrentRoleCatalogAction;
import com.ingot.cloud.iam.application.role.model.CurrentRoleGrantList;
import com.ingot.cloud.iam.application.role.model.RoleSubjectPage;
import com.ingot.cloud.iam.application.role.model.RoleSubjectSummary;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.assignment.TenantAssignmentOptionKind;
import com.ingot.cloud.iam.domain.organization.MemberDepartmentView;
import com.ingot.cloud.iam.domain.role.CurrentRoleDefinition;
import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>执行对应平台分配用例，逐项保留当前身份、来源和范围校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantAssignmentQueryService {
    private final TenantAssignmentOperations operationsContext;

    /** 分页读取允许披露的当前分配。 */
    public PageResponse<ResourceDetail<TenantAssignmentRecord>> list(int page,int size,SubjectType type,String keyword) {
        var admission=operationsContext.access().admit(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ);
        var actor=admission.actor(); IamPages.require(page,size);
        List<BigInteger> visible=admission.governed()?visible(actor,IamAction.TENANT_ASSIGNMENT_READ):null;
        List<BigInteger> sources=admission.governed()?null:operationsContext.live().mine(TenantAssignmentOperations.tenant(actor),TenantAssignmentOperations.member(actor)).stream().map(TenantAdministrationRows.Delegation::id).toList();
        var items=operationsContext.live().assignments(TenantAssignmentOperations.tenant(actor),visible,sources,type,search(keyword),size,IamPages.offset(page,size)).stream().map(row->operationsContext.detail(actor,row)).toList();
        return IamPages.details(items,operationsContext.live().assignmentsCount(TenantAssignmentOperations.tenant(actor),visible,sources,type,search(keyword)),page,size);
    }

    /**
     * 查询角色当前有效成员或用户组，组继承在 SQL 中展开并去重。
     *
     * @param roleId 可见角色 ID
     * @param type 成员或用户组
     * @param keyword 主体名称
     * @param page 页码
     * @param size 页大小
     * @return 可见主体；组读取受限时标记继承来源受限
     */
    public RoleSubjectPage subjects(String roleId, SubjectType type, String keyword, int page, int size) {
        operationsContext.roleService().get(false, roleId);
        var admission = operationsContext.access().admit(AuthorizationDomain.TENANT, IamAction.TENANT_ASSIGNMENT_READ);
        IamPages.require(page, size);
        var actor = admission.actor();
        var groups = visibleGroups(actor);
        var query = workspaceQuery(actor, admission, roleId, type, null, keyword, page, size, groups);
        var rows = operationsContext.workspace().page(query);
        var departments = type == SubjectType.MEMBER
                ? operationsContext.memberDepartments().visible(actor, rows.stream().map(TenantRoleWorkspaceMapper.Row::id).toList())
                : Map.<BigInteger, List<MemberDepartmentView>>of();
        var items = rows.stream().map(row -> new RoleSubjectSummary(row.id().toString(),
                row.name() == null || row.name().isBlank() ? row.id().toString() : row.name(), List.of(),
                sourceTypes(row.sourceTypes()), row.sourceCount(), Map.of(),
                departments.getOrDefault(row.id(), List.of()))).toList();
        return new RoleSubjectPage(items, operationsContext.workspace().count(query), page, size, groups != null);
    }

    /**
     * 查询一名成员持有该角色的直接分配和组继承来源。
     *
     * @param roleId 可见角色 ID
     * @param memberId 成员 ID
     * @param page 页码
     * @param size 页大小
     * @return 仍须按分配 ID 修改或撤销的来源页
     */
    public PageResponse<ResourceDetail<TenantAssignmentRecord>> sources(String roleId, String memberId, int page, int size) {
        operationsContext.roleService().get(false, roleId);
        var admission = operationsContext.access().admit(AuthorizationDomain.TENANT, IamAction.TENANT_ASSIGNMENT_READ);
        IamPages.require(page, size);
        var actor = admission.actor();
        var query = workspaceQuery(actor, admission, roleId, SubjectType.MEMBER, memberId, null, page, size, visibleGroups(actor));
        return IamPages.details(operationsContext.workspace().sources(query).stream().map(row -> operationsContext.detail(actor, row)).toList(),
                operationsContext.workspace().sourceCount(query), page, size);
    }

    /** 预览使用与提交相同的当前角色、范围和来源校验。 */
    public Preview<AssignmentPreviewResult> preview(TenantAssignmentBatchInput batch) {
        var operation=operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,false)?IamAction.TENANT_ASSIGNMENT_CREATE:IamAction.TENANT_ASSIGNMENT_UPDATE;
        var actor=operationsContext.access().require(AuthorizationDomain.TENANT,operation);
        operationsContext.validateBatchSource(actor,batch,operation);List<AssignmentPreviewItem> items=new ArrayList<>();List<ValidationIssue> errors=new ArrayList<>();
        for(var input:batch.items()) {try {operationsContext.validate(actor,input,operation);
            items.add(new AssignmentPreviewItem(input.subject(),true,List.of(),operationsContext.roles().definition(operationsContext.role(actor,input.roleId())).grants()));
        }catch(BizException ex){var reason=Arrays.stream(IamReasonCode.values()).filter(e->e.getCode().equals(ex.getCode())).findFirst().orElse(IamReasonCode.INVALID_ARGUMENT);
            if(reason==IamReasonCode.AUTHORIZATION_UNAVAILABLE)throw ex;var issue=new ValidationIssue(TenantAssignmentOperations.SCOPE_FIELD,reason,reason.getText());errors.add(issue);
            items.add(new AssignmentPreviewItem(input.subject(),false,List.of(issue),List.of()));}}
        return new Preview<>(actor.memberVersion(),errors.isEmpty(),errors,List.of(),new ImpactSummary((long)items.size(),(long)items.size(),0L,false),new AssignmentPreviewResult(items));
    }

    /** 查询当前可选角色、成员、组和资源，已选ID使用同一来源过滤。 */
    public PageResponse<TenantAssignmentOption> options(TenantAssignmentOptionKind kind,String source,String keyword,List<String> ids,int page,int size) {
        return options(kind,source,keyword,ids,page,size,null);
    }

    /** 组织成员浏览以部门条件与原直接/委派候选求交，不补充任何候选资格。 */
    public PageResponse<TenantAssignmentOption> options(TenantAssignmentOptionKind kind,String source,String keyword,List<String> ids,int page,int size,String departmentId) {
        return options(kind,source,keyword,ids,page,size,departmentId,null);
    }

    /** 组织人员浏览沿用直接/本人委派来源的原有候选资格。 */
    public PageResponse<TenantAssignmentOption> pickerOptions(String source, String keyword, int page, int size, com.ingot.cloud.iam.infrastructure.persistence.MemberPickerBranch branch) {
        return options(TenantAssignmentOptionKind.MEMBER,source,keyword,List.of(),page,size,null,branch);
    }

    /** 受限分配专用角色详情，不补通用角色读取资格。 */
    public CurrentRoleGrantList grants(String id,String source) {
        var actor=optionActor();candidateAdmission(actor,TenantAssignmentOptionKind.ROLE);var grant=operationsContext.source(actor,source,null);var role=operationsContext.role(actor,id);
        if(grant!=null&&!operationsContext.live().delegatedRoles(grant.id()).contains(role.id()))TenantAssignmentOperations.denied();
        var opened=operationsContext.live().applications(TenantAssignmentOperations.tenant(actor)).stream().map(TenantAdministrationRows.Application::id).collect(java.util.stream.Collectors.toSet());
        var list=operationsContext.roleService().grantsForAssignment(actor,id);
        var actions=list.items().stream().filter(CurrentRoleCatalogAction::enabled).filter(a->opened.contains(new BigInteger(a.applicationId()))).toList();
        var allowed=actions.stream().map(CurrentRoleCatalogAction::actionId).collect(java.util.stream.Collectors.toSet());
        var grants=list.definition().grants().stream().filter(g->allowed.contains(g.actionId())).toList();
        var used=grants.stream().flatMap(g->g.scopes().stream()).map(ScopeExpression::parameterKey).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        return new CurrentRoleGrantList(actions,new CurrentRoleDefinition(grants,list.definition().parameterDefinitions().stream().filter(p->used.contains(p.key())).toList(),list.definition().resourceFieldPermissions()));
    }

    /** 当前角色参数候选；未知业务资源返回明确不支持。 */
    public AuthorizationCandidatePage scopeCandidates(String roleId,String key,String source,String keyword,List<String> selected,int page,int size,boolean tree,String parent) {
        var actor=optionActor();var grant=operationsContext.source(actor,source,null);var role=operationsContext.role(actor,roleId);
        if(grant!=null&&!operationsContext.live().delegatedRoles(grant.id()).contains(role.id()))TenantAssignmentOperations.denied();
        var definition=operationsContext.roles().definition(role);var parameter=definition.parameterDefinitions().stream().filter(p->p.key().equals(key)).findFirst().orElseThrow(()->new BizException(IamReasonCode.INVALID_ARGUMENT));
        return candidates(actor,parameter,grant,keyword,selected,page,size,tree,parent);
    }

    private PageResponse<TenantAssignmentOption> options(TenantAssignmentOptionKind kind,String source,String keyword,List<String> ids,int page,int size,String departmentId,com.ingot.cloud.iam.infrastructure.persistence.MemberPickerBranch branch) {
        var actor=optionActor();candidateAdmission(actor,kind);var grant=operationsContext.source(actor,source,null);if(grant!=null&&(kind==TenantAssignmentOptionKind.APPLICATION||kind==TenantAssignmentOptionKind.DEPARTMENT))TenantAssignmentOperations.denied();IamPages.require(page,size);
        BigInteger department = kind == TenantAssignmentOptionKind.MEMBER && departmentId != null && !departmentId.isBlank() && (keyword == null || keyword.isBlank()) ? BigInteger.valueOf(IamIds.require(departmentId)) : null;
        if (department != null) { operationsContext.access().require(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_READ);operationsContext.resources().requireVisibleDepartment(actor.context(),IamAction.TENANT_DEPARTMENT_READ,department.longValueExact()); }

        var query=new TenantAuthorizationOptionSql.Query(TenantAssignmentOperations.tenant(actor),TenantAssignmentOperations.member(actor),grant==null?null:grant.id(),kind,search(keyword),ids(ids),size,IamPages.offset(page,size),department,branch);
        var options=operationsContext.live().options(query);
        var views=kind==TenantAssignmentOptionKind.MEMBER?operationsContext.memberDepartments().visible(actor,options.stream().map(TenantAuthorizationOptionSql.Option::id).toList()):Map.<BigInteger,List<MemberDepartmentView>>of();
        return new PageResponse<>(options.stream().map(o->new TenantAssignmentOption(o.id().toString(),o.name(),kind,o.roleKind(),views.getOrDefault(o.id(),List.of()))).toList(),operationsContext.live().optionsCount(query),page,size);
    }

    /** 按真实资源及参数查询范围候选，供独立委派治理复用。 */
    private AuthorizationCandidatePage candidates(ActiveIdentity actor,CurrentRoleParameter parameter,TenantAdministrationRows.Delegation source,
        String keyword,List<String> selected,int page,int size,boolean tree,String parent) {
        return operationsContext.scopeBindings().candidates(actor, parameter, source, keyword, selected, page, size, tree, parent);
    }
    private void candidateAdmission(ActiveIdentity actor,TenantAssignmentOptionKind kind) {
        var rights=operationsContext.access().capabilities(actor,List.of(IamAction.TENANT_ASSIGNMENT_READ,IamAction.TENANT_ASSIGNMENT_CREATE,IamAction.TENANT_ASSIGNMENT_UPDATE,IamAction.TENANT_DELEGATION_CREATE,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_ROLE_CREATE,IamAction.TENANT_ROLE_UPDATE,IamAction.TENANT_ROLE_PREVIEW));
        boolean delegation=rights.allows(IamAction.TENANT_DELEGATION_CREATE,true)||rights.allows(IamAction.TENANT_DELEGATION_UPDATE,true);
        boolean assignment=rights.allows(IamAction.TENANT_ASSIGNMENT_READ,false)||rights.allows(IamAction.TENANT_ASSIGNMENT_CREATE,false)||rights.allows(IamAction.TENANT_ASSIGNMENT_UPDATE,false);
        boolean role=rights.allows(IamAction.TENANT_ROLE_CREATE,true)||rights.allows(IamAction.TENANT_ROLE_UPDATE,true)||rights.allows(IamAction.TENANT_ROLE_PREVIEW,true);
        if(kind==TenantAssignmentOptionKind.DEPARTMENT ? !delegation : !(assignment||delegation||role&&(kind==TenantAssignmentOptionKind.APPLICATION||kind==TenantAssignmentOptionKind.RESOURCE)))TenantAssignmentOperations.denied();
    }
    private ActiveIdentity optionActor(){var actor=operationsContext.access().requireCurrent();if(actor.context().domain()!=AuthorizationDomain.TENANT)TenantAssignmentOperations.denied();if(!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ,false)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,false)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,false)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_CREATE,true)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_UPDATE,true)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_PREVIEW,true))TenantAssignmentOperations.denied();return actor;}
    private List<BigInteger> visible(ActiveIdentity actor,IamAction action){var scope=operationsContext.resources().objects(actor.context(),action);return scope.coversAll()?null:scope.clauses().stream().flatMap(c->c.requiredIds().stream()).distinct().toList();}
    private List<BigInteger> visibleGroups(ActiveIdentity actor) {
        if (!operationsContext.access().capabilities(actor, List.of(IamAction.TENANT_GROUP_READ)).allows(IamAction.TENANT_GROUP_READ, false)) return List.of();
        var scope = operationsContext.resources().objects(actor.context(), IamAction.TENANT_GROUP_READ);
        if (scope.coversAll()) return null;
        return scope.clauses().stream().filter(clause -> clause.departmentSets().isEmpty())
                .flatMap(clause -> clause.requiredIds().stream()).distinct().toList();
    }
    private TenantRoleWorkspaceSql.Query workspaceQuery(ActiveIdentity actor, AuthorizationAdmission admission, String roleId,
            SubjectType type, String memberId, String keyword, int page, int size, List<BigInteger> groups) {
        List<BigInteger> visible = admission.governed() ? visible(actor, IamAction.TENANT_ASSIGNMENT_READ) : null;
        List<BigInteger> sources = admission.governed() ? null : operationsContext.live().mine(TenantAssignmentOperations.tenant(actor), TenantAssignmentOperations.member(actor)).stream().map(TenantAdministrationRows.Delegation::id).toList();
        String name = IamFilters.containsName(keyword);
        String search = name == null ? "%" : "%" + name.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return new TenantRoleWorkspaceSql.Query(TenantAssignmentOperations.tenant(actor), BigInteger.valueOf(IamIds.require(roleId)),
                memberId == null ? null : BigInteger.valueOf(IamIds.require(memberId)), type, visible, sources, groups,
                search, IamPages.offset(page, size), size);
    }
    private static List<SubjectType> sourceTypes(String json) {
        if (json == null || json.isBlank()) return List.of();
        return IamJsonCodec.read(json, TenantAssignmentOperations.SOURCE_TYPES).stream().map(SubjectType::valueOf).distinct().sorted().toList();
    }
    private static List<BigInteger> ids(List<String> values){if(values==null)return List.of();if(values.size()>IamPages.MAX_SIZE)TenantAssignmentOperations.invalid();return values.stream().map(v->BigInteger.valueOf(IamIds.require(v))).distinct().toList();}
    private static String search(String v){return "%"+(v==null?"":v.trim()).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
}
