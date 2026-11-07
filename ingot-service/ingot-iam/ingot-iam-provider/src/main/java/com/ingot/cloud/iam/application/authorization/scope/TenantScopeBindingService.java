package com.ingot.cloud.iam.application.authorization.scope;

import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.cloud.iam.domain.assignment.TenantScopeObjectResource;
import com.ingot.cloud.iam.domain.authorization.ScopeBinder;
import com.ingot.cloud.iam.domain.authorization.ScopeClause;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.infrastructure.persistence.TenantAuthorizationOptionSql;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateSql;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationOption;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.cloud.iam.domain.assignment.TenantAssignmentOptionKind;
import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import com.ingot.cloud.iam.domain.tenant.TenantAuthorizationConstants;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按同一资源和委派范围规则查询候选并校验参数绑定。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantScopeBindingService {

    private final TenantAdministrationMapper live;
    private final TenantScopeCandidateMapper candidates;
    private final DepartmentClosure departments;
    private static final TypeReference<Map<String,ScopeBinding>> BINDINGS = new TypeReference<>() { };
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() { };

    /** 按真实资源及参数查询范围候选，供独立委派治理复用。 */
    public AuthorizationCandidatePage candidates(ActiveIdentity actor,CurrentRoleParameter parameter,TenantAdministrationRows.Delegation source,
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
    /** 复用命名参数的资源解析与委派上限，不授予额外对象范围。 */
    public List<BigInteger> allowedBindings(ActiveIdentity actor,CurrentRoleParameter parameter,TenantAdministrationRows.Delegation source) {
        var limit=live.ceilings(source.id()).stream().filter(c->c.resourceId().toString().equals(parameter.resourceId())).findFirst().orElse(null);
        if(limit==null)return List.of();
        var clauses=ScopeBinder.bind(new ActionScopeCeiling(parameter.resourceId(),IamJsonCodec.read(limit.scopes(),SCOPES),IamJsonCodec.read(limit.scopeBindings(),BINDINGS)));
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
                if(recipients==null)recipients=live.scopeRecipients(new TenantAuthorizationOptionSql.Query(tenant(actor),source.administratorMemberId(),source.id(),TenantAssignmentOptionKind.MEMBER,"%",List.of(),IamPages.DEFAULT_SIZE,0));
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

    /** 按资源真实归属校验全部命名绑定。 */
    public void validateBindings(ActiveIdentity actor,List<CurrentRoleParameter> parameters,Map<String,ScopeBinding> bindings) {
        if(bindings==null||bindings.size()!=parameters.size())invalid();
        for(var p:parameters){var b=bindings.get(p.key());if(b==null||b.kind()!=p.kind()||b.ids()==null)invalid();
            var target=target(p);if(target==null&&!b.ids().isEmpty())invalid();if(b.ids().isEmpty())continue;
            var values=ids(b.ids());var q=new TenantScopeCandidateSql.Query(target,tenant(actor).longValueExact(),"%",values,0,IamPages.MAX_SIZE);
            if(candidates.count(q)!=values.size())invalid();}
    }
    /** 复用命名参数的资源解析与委派上限，不授予额外对象范围。 */
    public TenantScopeObjectResource target(CurrentRoleParameter p){if(p.kind()==ScopeBindingKind.DEPARTMENTS)return TenantScopeObjectResource.DEPARTMENT;var row=live.resource(new BigInteger(p.resourceId()));return row==null||!TenantAuthorizationConstants.CORE_APPLICATION.equals(row.applicationCode())?null:TenantScopeObjectResource.find(row.resourceCode());}
    private static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
    private static List<BigInteger> ids(List<String> values){if(values==null)return List.of();if(values.size()>IamPages.MAX_SIZE)invalid();return values.stream().map(v->BigInteger.valueOf(IamIds.require(v))).distinct().toList();}
    private static String search(String v){return "%"+(v==null?"":v.trim()).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private static void invalid(){throw new BizException(IamReasonCode.INVALID_ARGUMENT);}
}
