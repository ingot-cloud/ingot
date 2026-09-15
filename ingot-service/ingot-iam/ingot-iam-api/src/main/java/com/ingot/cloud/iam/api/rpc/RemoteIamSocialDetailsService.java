package com.ingot.cloud.iam.api.rpc;

import java.util.List;

import com.ingot.cloud.iam.api.model.domain.SysSocialDetails;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>声明 IAM 社交认证配置查询 RPC，仅供内部服务使用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@FeignClient(contextId = "iamSocialDetailsService", value = ServiceNameConstants.IAM_SERVICE)
public interface RemoteIamSocialDetailsService {

    /**
     * 查询指定社交认证类型的内部配置。
     * @param type 已支持的认证类型
     * @return 内部配置列表
     */
    @GetMapping("/inner/social/detailList/{type}")
    R<List<SysSocialDetails>> getSocialDetailsByType(@PathVariable String type);

    /**
     * 按应用标识查询社交认证内部配置。
     * @param appId 应用标识
     * @return 内部配置
     */
    @GetMapping("/inner/social/appId/{appId}")
    R<SysSocialDetails> getDetailsByAppId(@PathVariable String appId);
}
