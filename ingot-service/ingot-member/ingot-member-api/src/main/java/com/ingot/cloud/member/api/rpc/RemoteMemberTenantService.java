package com.ingot.cloud.member.api.rpc;

import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.common.TenantBaseDTO;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>Description  : MemberTenantService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 08:25.</p>
 */
@FeignClient(contextId = "memberTenantService", value = ServiceNameConstants.MEMBER_SERVICE)
public interface RemoteMemberTenantService {

    @DeleteMapping("/inner/tenant/{id}")
    R<Void> deleteTenant(@PathVariable Long id);

    @PutMapping("/inner/tenant/base")
    R<Void> updateTenantBase(@RequestBody TenantBaseDTO params);

}
