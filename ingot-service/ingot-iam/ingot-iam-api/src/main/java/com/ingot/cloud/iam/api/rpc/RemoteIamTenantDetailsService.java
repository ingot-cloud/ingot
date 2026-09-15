package com.ingot.cloud.iam.api.rpc;

import java.util.List;

import com.ingot.cloud.iam.api.model.domain.SysTenant;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>声明 IAM 租户信息查询 RPC，调用方仍须验证服务身份与数据范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "iamTenantDetailsService", value = ServiceNameConstants.IAM_SERVICE)
public interface RemoteIamTenantDetailsService {

    /**
     * 查询账号关联的租户摘要。
     * @param username 账号登录名
     * @return 关联租户摘要
     */
    @PostMapping(value = "/inner/tenant/details/{username}")
    R<TenantDetailsResponse> getUserTenantDetails(@PathVariable("username") String username);

    /**
     * 批量查询指定租户摘要。
     * @param ids 租户 ID 集合
     * @return 租户摘要集合
     */
    @PostMapping("/inner/tenant/detailsList")
    R<TenantDetailsResponse> getTenantByIds(@RequestBody List<Long> ids);

    /**
     * 查询指定租户实体供内部调用。
     * @param id 租户 ID
     * @return 租户实体
     */
    @GetMapping("/inner/tenant/{id}")
    R<SysTenant> getTenantById(@PathVariable Long id);
}
