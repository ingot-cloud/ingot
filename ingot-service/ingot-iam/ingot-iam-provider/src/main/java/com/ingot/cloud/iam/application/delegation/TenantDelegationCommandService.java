package com.ingot.cloud.iam.application.delegation;

import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationInput;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationRecord;
import com.ingot.cloud.iam.application.delegation.model.TenantDelegationUpdateInput;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
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
 * <p>执行对应租户委派用例，保留事务锁和授权上限。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantDelegationCommandService {
    private final TenantDelegationOperations operationsContext;

    /** 原子创建完整委派。 */
    public CreatedResource create(TenantDelegationInput input){var actor=operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE);return operationsContext.transactions().execute(tx->{lock(actor,IamAction.TENANT_DELEGATION_CREATE);if(!operationsContext.resources().objects(actor.context(),IamAction.TENANT_DELEGATION_CREATE).coversAll())throw new BizException(IamReasonCode.ACTION_DENIED);operationsContext.roleLocks().lockRoles(actor,input.allowedRoleIds());operationsContext.validate(actor,input);var id=BigInteger.valueOf(operationsContext.access().nextId());applied(operationsContext.live().insertDelegation(row(actor,id,BigInteger.ZERO,input)));save(actor,id,input);audit(actor,id,null,input,BigInteger.ZERO,AuditChangeType.CREATE);operationsContext.changes().markAll();return new CreatedResource(id.toString(),"0");});}
    /** 原子更新限制，所有派生分配下一请求执行当前上限。 */
    public ResourceDetail<TenantDelegationRecord> replace(String id,TenantDelegationUpdateInput input){var actor=operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE);return operationsContext.transactions().execute(tx->{lock(actor,IamAction.TENANT_DELEGATION_UPDATE);operationsContext.roleLocks().lockRoles(actor,input.delegation().allowedRoleIds());operationsContext.resources().requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_UPDATE,IamIds.require(id));var old=operationsContext.require(actor,id,true);IamIds.requireVersion(input.expectedVersion(),old.version().toString());var before=operationsContext.input(actor,old);operationsContext.validate(actor,input.delegation());applied(operationsContext.live().updateDelegation(row(actor,old.id(),old.version(),input.delegation())));save(actor,old.id(),input.delegation());audit(actor,old.id(),before,input.delegation(),old.version().add(BigInteger.ONE),AuditChangeType.UPDATE);operationsContext.changes().markAll();return operationsContext.detail(actor,operationsContext.live().delegation(TenantDelegationOperations.tenant(actor),old.id()));});}
    /** 撤销来源并保留派生记录与审计。 */
    public CreatedResource delete(String id){var actor=operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_DELETE);return operationsContext.transactions().execute(tx->{lock(actor,IamAction.TENANT_DELEGATION_DELETE);operationsContext.resources().requireVisibleObject(actor.context(),IamAction.TENANT_DELEGATION_DELETE,IamIds.require(id));var old=operationsContext.require(actor,id,true);applied(operationsContext.live().revokeDelegation(TenantDelegationOperations.tenant(actor),old.id(),old.version()));audit(actor,old.id(),operationsContext.input(actor,old),operationsContext.input(actor,old),old.version().add(BigInteger.ONE),AuditChangeType.REVOKE);operationsContext.changes().markAll();return new CreatedResource(id,old.version().add(BigInteger.ONE).toString());});}
    private void save(ActiveIdentity actor,BigInteger id,TenantDelegationInput input){operationsContext.live().clearAllowedRoles(id);operationsContext.live().clearCeilings(id);operationsContext.live().clearRecipientMembers(TenantDelegationOperations.tenant(actor),id);operationsContext.live().clearRecipientDepartments(TenantDelegationOperations.tenant(actor),id);
        for(var roleId:input.allowedRoleIds()){var role=operationsContext.live().role(TenantDelegationOperations.tenant(actor),new BigInteger(roleId));operationsContext.live().allowedRole(id,TenantDelegationOperations.tenant(actor),role,role.tenantId()==null?BigInteger.ZERO:TenantDelegationOperations.tenant(actor));}
        for(var c:input.resourceScopeCeilings())operationsContext.live().ceiling(id,new BigInteger(c.resourceId()),IamJsonCodec.array(c.scopes()),IamJsonCodec.object(c.scopeBindings()));
        for(var member:input.recipientSelection().members())operationsContext.live().recipientMember(BigInteger.valueOf(operationsContext.access().nextId()),TenantDelegationOperations.tenant(actor),id,new BigInteger(member));for(var d:input.recipientSelection().departments())operationsContext.live().recipientDepartment(TenantDelegationOperations.tenant(actor),id,new BigInteger(d.id()),Boolean.TRUE.equals(d.includeDescendants()));}
    private TenantAdministrationRows.Delegation row(ActiveIdentity actor,BigInteger id,BigInteger version,TenantDelegationInput input){return new TenantAdministrationRows.Delegation(id,TenantDelegationOperations.tenant(actor),new BigInteger(input.administratorMemberId()),time(input.validFrom()),time(input.validUntil()),BigInteger.valueOf(input.maxAssignmentDuration().getSeconds()),input.maxAssignmentDuration().getNano(),GrantStatus.ACTIVE,version,null,null);}
    private void lock(ActiveIdentity actor,IamAction action){if(operationsContext.live().lockTenant(TenantDelegationOperations.tenant(actor))==null)TenantDelegationOperations.missing();operationsContext.access().requireGoverned(AuthorizationDomain.TENANT,action);}
    private void audit(ActiveIdentity actor,BigInteger id,TenantDelegationInput before,TenantDelegationInput after,BigInteger version,AuditChangeType type) {
        operationsContext.audits().write(actor.context(),operationsContext.access().nextId(),TenantDelegationOperations.TARGET,id.toString(),type,before==null?Map.of():safe(before,GrantStatus.ACTIVE),safe(after,type==AuditChangeType.REVOKE?GrantStatus.REVOKED:GrantStatus.ACTIVE),Map.of(TenantDelegationOperations.TARGET,version.toString()),id.toString(),null);
    }
    private Map<AuditField,Object> safe(TenantDelegationInput input,GrantStatus status) {
        var result=new LinkedHashMap<AuditField,Object>();
        result.put(AuditField.SCOPE,new TenantDelegationOperations.AuditScope(input.administratorMemberId(),input.allowedRoleIds(),input.resourceScopeCeilings(),input.maxAssignmentDuration().toString()));
        result.put(AuditField.RECIPIENT_SELECTION,input.recipientSelection());result.put(AuditField.STATUS,status.getValue());
        if(input.validFrom()!=null)result.put(AuditField.VALID_FROM,input.validFrom().toString());if(input.validUntil()!=null)result.put(AuditField.VALID_UNTIL,input.validUntil().toString());
        return result;
    }
    private static LocalDateTime time(Instant i){return i==null?null:LocalDateTime.ofInstant(i,ZoneOffset.UTC);}
    private static void applied(int n){if(n!=1)throw new BizException(IamReasonCode.REVISION_CONFLICT);}
}
