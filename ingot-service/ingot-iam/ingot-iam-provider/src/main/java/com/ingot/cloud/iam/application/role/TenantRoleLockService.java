package com.ingot.cloud.iam.application.role;

import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按统一顺序锁定并核对角色归属，供分配与委派复用。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantRoleLockService {

    private final TenantAdministrationMapper live;
    /** 锁定批次角色头，防止校验期间共享权限发生切换；统一按角色ID排序。 */
    public void lockRoles(ActiveIdentity actor,List<String> roleIds) {
        roleIds.stream().map(BigInteger::new).distinct().sorted().forEach(id->{
            var row=live.lockRole(id);
            if(row==null || row.kind()!=RoleKind.SHARED && (row.kind()!=RoleKind.TENANT_CUSTOM || !tenant(actor).equals(row.tenantId())))missing();
        });
    }
    private static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
    private static void missing(){throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);}
}
