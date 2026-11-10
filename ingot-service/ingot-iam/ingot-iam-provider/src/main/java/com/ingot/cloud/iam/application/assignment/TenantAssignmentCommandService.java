package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.assignment.model.SubjectRef;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentRecord;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentUpdateInput;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.domain.assignment.AssignmentSource;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
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
 * <p>执行对应平台分配用例，逐项保留当前身份、来源和范围校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantAssignmentCommandService {
    private final TenantAssignmentOperations operationsContext;

    /** 原子提交批次，受限管理员只能使用同一条本人有效委派。 */
    public CreatedResource create(TenantAssignmentBatchInput batch) {
        var actor=operationsContext.access().require(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE);
        return operationsContext.transactions().execute(tx->{ lock(actor,IamAction.TENANT_ASSIGNMENT_CREATE); lockRoles(actor,batch.items().stream().map(TenantAssignmentInput::roleId).toList()); operationsContext.validateBatchSource(actor,batch,IamAction.TENANT_ASSIGNMENT_CREATE);
            long first=0; for(var input:batch.items()) {
                operationsContext.validate(actor,input,IamAction.TENANT_ASSIGNMENT_CREATE); long id=operationsContext.access().nextId();if(first==0)first=id;
                var role=operationsContext.role(actor,input.roleId()); var row=row(actor,id,BigInteger.ZERO,input,role);
                applied(operationsContext.live().insertAssignment(row,role.tenantId()==null?BigInteger.ZERO:TenantAssignmentOperations.tenant(actor)));
                audit(actor,null,row,AuditChangeType.CREATE); }
            operationsContext.changes().markAll();return new CreatedResource(Long.toString(first),"0");});
    }

    /** 调整完整当前分配，来源不能由受限管理员替换。 */
    public ResourceDetail<TenantAssignmentRecord> replace(String id,TenantAssignmentUpdateInput input) {
        var actor=operationsContext.access().require(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE);
        var assignmentId = BigInteger.valueOf(IamIds.require(id));
        return operationsContext.transactions().execute(tx->{lock(actor,IamAction.TENANT_ASSIGNMENT_UPDATE);lockRoles(actor,List.of(input.assignment().roleId()));var old=require(actor,assignmentId);
            authorizeExisting(actor,old,IamAction.TENANT_ASSIGNMENT_UPDATE);IamIds.requireVersion(input.expectedVersion(),old.version().toString());
            if(!operationsContext.access().allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,true)
                && !Objects.equals(old.delegationGrantId()==null?null:old.delegationGrantId().toString(),input.assignment().delegationGrantId()))TenantAssignmentOperations.denied();
            operationsContext.validate(actor,input.assignment(),IamAction.TENANT_ASSIGNMENT_UPDATE);var role=operationsContext.role(actor,input.assignment().roleId());
            var next=row(actor,old.id().longValueExact(),old.version(),input.assignment(),role);
            applied(operationsContext.live().updateAssignment(next,role.tenantId()==null?BigInteger.ZERO:TenantAssignmentOperations.tenant(actor)));audit(actor,old,next,AuditChangeType.UPDATE);operationsContext.changes().markAll();
            return operationsContext.detail(actor,operationsContext.live().assignment(TenantAssignmentOperations.tenant(actor),old.id()));});
    }

    /** 撤销分配并保留历史。 */
    public CreatedResource delete(String id) {
        var actor=operationsContext.access().require(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_DELETE);
        var assignmentId = BigInteger.valueOf(IamIds.require(id));
        return operationsContext.transactions().execute(tx->{lock(actor,IamAction.TENANT_ASSIGNMENT_DELETE);var old=require(actor,assignmentId);
            authorizeExisting(actor,old,IamAction.TENANT_ASSIGNMENT_DELETE);applied(operationsContext.live().revokeAssignment(TenantAssignmentOperations.tenant(actor),old.id(),old.version()));
            audit(actor,old,old,AuditChangeType.REVOKE);operationsContext.changes().markAll();return new CreatedResource(id,old.version().add(BigInteger.ONE).toString());});
    }
    private TenantAdministrationRows.Assignment require(ActiveIdentity actor, BigInteger id) {
        var row = operationsContext.live().lockAssignment(TenantAssignmentOperations.tenant(actor), id);
        if (row == null) TenantAssignmentOperations.missing();
        return row;
    }
    private void authorizeExisting(ActiveIdentity actor,TenantAdministrationRows.Assignment row,IamAction action){if(operationsContext.access().allows(AuthorizationDomain.TENANT,action,true))operationsContext.resources().requireVisibleObject(actor.context(),action,row.id().longValueExact());else operationsContext.source(actor,row.delegationGrantId()==null?null:row.delegationGrantId().toString(),action);}
    /** 锁定批次角色头，防止校验期间共享权限发生切换；统一按角色ID排序。 */
    private void lockRoles(ActiveIdentity actor,List<String> roleIds) {
        operationsContext.roleLocks().lockRoles(actor, roleIds);
    }
    private void lock(ActiveIdentity actor,IamAction action){if(operationsContext.live().lockTenant(TenantAssignmentOperations.tenant(actor))==null)TenantAssignmentOperations.missing();operationsContext.access().require(AuthorizationDomain.TENANT,action);}
    private TenantAdministrationRows.Assignment row(ActiveIdentity actor,long id,BigInteger version,TenantAssignmentInput input,TenantAdministrationRows.Role role){return new TenantAdministrationRows.Assignment(BigInteger.valueOf(id),TenantAssignmentOperations.tenant(actor),input.subject().type(),input.subject().type()==SubjectType.MEMBER?new BigInteger(input.subject().id()):null,input.subject().type()==SubjectType.GROUP?new BigInteger(input.subject().id()):null,role.id(),role.kind(),role.currentRevisionId(),IamJsonCodec.object(input.scopeBindings()),input.delegationGrantId()==null?null:new BigInteger(input.delegationGrantId()),time(input.validFrom()==null?Instant.now():input.validFrom()),time(input.validUntil()),GrantStatus.ACTIVE,AssignmentSource.MANUAL,version,null,null,null);}
    private void audit(ActiveIdentity actor,TenantAdministrationRows.Assignment before,TenantAdministrationRows.Assignment after,AuditChangeType type) {
        operationsContext.audits().write(actor.context(),operationsContext.access().nextId(),TenantAssignmentOperations.TARGET,after.id().toString(),type,before==null?Map.of():safe(before,before.status()),safe(after,type==AuditChangeType.REVOKE?GrantStatus.REVOKED:after.status()),Map.of(TenantAssignmentOperations.TARGET,after.version().add(type==AuditChangeType.CREATE?BigInteger.ZERO:BigInteger.ONE).toString()),after.delegationGrantId()==null?null:after.delegationGrantId().toString(),after.id().toString());
    }
    private Map<AuditField,Object> safe(TenantAdministrationRows.Assignment row,GrantStatus status) {
        var result=new LinkedHashMap<AuditField,Object>();
        result.put(AuditField.ROLE,row.roleId().toString());result.put(AuditField.ROLE_REVISION,row.currentRevisionId().toString());
        result.put(AuditField.RECIPIENT_SELECTION,new SubjectRef(row.subjectType(),(row.subjectType()==SubjectType.MEMBER?row.memberId():row.groupId()).toString()));
        result.put(AuditField.SCOPE,IamJsonCodec.read(row.scopeBindings(),TenantAssignmentOperations.BINDINGS));result.put(AuditField.STATUS,status.getValue());
        result.put(AuditField.VALID_FROM,TenantAssignmentOperations.instant(row.validFrom()).toString());if(row.validUntil()!=null)result.put(AuditField.VALID_UNTIL,TenantAssignmentOperations.instant(row.validUntil()).toString());
        return result;
    }
    private static LocalDateTime time(Instant i){return i==null?null:LocalDateTime.ofInstant(i,ZoneOffset.UTC);}
    private static void applied(int count){if(count!=1)throw new BizException(IamReasonCode.REVISION_CONFLICT);}
}
