package com.ingot.cloud.iam.application.delegation;

import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationMine;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationRecord;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationUpdateInput;
import com.ingot.cloud.iam.application.role.model.CurrentRoleSummary;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.shared.model.ReferenceImpactPreview;
import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import com.ingot.cloud.iam.domain.shared.ConfigurationStatus;
import com.ingot.cloud.iam.domain.tenant.TenantAuthorizationConstants;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>执行对应租户委派用例，保留事务锁和授权上限。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantDelegationQueryService {
    private final TenantDelegationOperations operationsContext;

    /** 治理范围内分页查询委派。 */
    public PageResponse<ResourceDetail<TenantDelegationRecord>> list(int page,int size,String keyword){
        var actor=operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_READ);IamPages.require(page,size);
        var scope=operationsContext.resources().objects(actor.context(),IamAction.TENANT_DELEGATION_READ);
        var visible=scope.coversAll()?null:scope.clauses().stream().flatMap(c->c.requiredIds().stream()).distinct().toList();
        String search=keyword==null?null:"%"+keyword.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return IamPages.details(operationsContext.live().delegations(TenantDelegationOperations.tenant(actor),visible,search,size,IamPages.offset(page,size)).stream().map(d->operationsContext.detail(actor,d)).toList(),operationsContext.live().delegationsCount(TenantDelegationOperations.tenant(actor),visible,search),page,size);
    }
    /** 读取可见委派，委派入口不授予独立治理资格。 */
    public ResourceDetail<TenantDelegationRecord> get(String id){var actor=operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_READ);operationsContext.resources().requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_READ,IamIds.require(id));return operationsContext.detail(actor,operationsContext.require(actor,id,false));}
    /** 校验拟更新委派，受影响细节按当前可见分配范围披露。 */
    public Preview<ReferenceImpactPreview> preview(String id,TenantDelegationUpdateInput input){var actor=operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_PREVIEW);operationsContext.resources().requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_PREVIEW,IamIds.require(id));var old=operationsContext.require(actor,id,false);IamIds.requireVersion(input.expectedVersion(),old.version().toString());operationsContext.validate(actor,input.delegation());var summary=new ImpactSummary(null,null,null,true);return new Preview<>(old.version().toString(),true,List.of(),List.of(),summary,new ReferenceImpactPreview(List.of(),summary));}
    /** 返回本人有效委派摘要，不依赖委派管理操作。 */
    public List<TenantDelegationMine> mine(){var actor=operationsContext.access().requireCurrent();if(actor.context().domain()!=AuthorizationDomain.TENANT)throw new BizException(IamReasonCode.ACTION_DENIED);var rights=operationsContext.access().capabilities(actor,List.of(IamAction.TENANT_ASSIGNMENT_CREATE,IamAction.TENANT_ASSIGNMENT_UPDATE));var direct=List.of(IamAction.TENANT_ASSIGNMENT_CREATE,IamAction.TENANT_ASSIGNMENT_UPDATE).stream().filter(operation->rights.allows(operation,true)).map(IamAction::getCode).toList();return operationsContext.live().mine(TenantDelegationOperations.tenant(actor),new BigInteger(actor.context().memberId())).stream().map(d->{var input=operationsContext.input(actor,d);var allowed=input.allowedRoleIds().stream().map(id->operationsContext.live().role(TenantDelegationOperations.tenant(actor),new BigInteger(id))).filter(Objects::nonNull).map(role->new CurrentRoleSummary(role.id().toString(),role.code(),role.name(),role.description(),role.groupName(),role.kind(),Boolean.TRUE.equals(role.enabled())?ConfigurationStatus.ENABLED:ConfigurationStatus.DISABLED)).toList();return new TenantDelegationMine(d.id().toString(),allowed,"指定成员与部门",TenantDelegationOperations.instant(d.validFrom()),TenantDelegationOperations.instant(d.validUntil()),input.maxAssignmentDuration(),operationsContext.issues(actor,input),direct);}).toList();}
    /** 独立治理者按实际资源参数查询范围对象。 */
    public AuthorizationCandidatePage scopeCandidates(String resourceId,String key,String keyword,List<String> ids,int page,int size){var actor=operationsContext.access().requireCurrent();if(!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)&&!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true))throw new BizException(IamReasonCode.ACTION_DENIED);var parameter=parameter(actor,resourceId,key);return operationsContext.scopeBindings().candidates(actor,parameter,null,keyword,ids,page,size,false,null);}

    private CurrentRoleParameter parameter(ActiveIdentity actor,String resourceId,String key) {
        if(operationsContext.live().openResource(TenantDelegationOperations.tenant(actor),new BigInteger(resourceId))!=1)throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var supported=IamJsonCodec.read(operationsContext.live().resourceScopeKinds(new BigInteger(resourceId)),new TypeReference<List<ScopeKind>>() { });
        if(key.equals(TenantAuthorizationConstants.resourceParameterKey(resourceId,ScopeBindingKind.DEPARTMENTS)) && supported.contains(ScopeKind.MANAGED_DEPARTMENTS))
            return new CurrentRoleParameter(key,ScopeBindingKind.DEPARTMENTS,resourceId);
        if(key.equals(TenantAuthorizationConstants.resourceParameterKey(resourceId,ScopeBindingKind.OBJECTS)) && supported.contains(ScopeKind.OBJECT_SET))
            return new CurrentRoleParameter(key,ScopeBindingKind.OBJECTS,resourceId);
        var parameters=operationsContext.live().currentParameters(TenantDelegationOperations.tenant(actor),new BigInteger(resourceId),key);
        if(parameters.size()!=1)throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var parameter=parameters.getFirst();return new CurrentRoleParameter(parameter.key(),parameter.kind(),resourceId);
    }
}
