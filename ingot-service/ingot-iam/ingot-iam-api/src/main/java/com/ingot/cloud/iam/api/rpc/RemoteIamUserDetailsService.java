package com.ingot.cloud.iam.api.rpc;

import java.util.List;

import com.ingot.cloud.iam.api.model.dto.user.InnerUserDTO;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * <p>声明 IAM 认证资料查询 RPC，敏感认证数据不对前端暴露。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@FeignClient(contextId = "iamUserDetailsService", value = ServiceNameConstants.IAM_SERVICE)
public interface RemoteIamUserDetailsService {

    /**
     * 查询内部认证资料，业务权限仍须按当前成员身份计算。
     * @param params 账号认证查询条件
     * @return 内部认证响应，不能直接返回给前端
     */
    @PostMapping(value = "/inner/user/details")
    R<UserDetailsResponse> getUserAuthDetails(@RequestBody UserDetailsRequest params);

    /**
     * 查询指定账号的内部资料。
     * @param id 账号 ID
     * @return 账号资料
     */
    @GetMapping("/inner/user/{id}")
    R<InnerUserDTO> getUserInfo(@PathVariable Long id);

    /**
     * 批量查询用户信息。
     *
     * <p>服务端为 {@code POST /inner/user/list}，ID 集合走请求体，不能声明成 GET。</p>
     */
    @PostMapping("/inner/user/list")
    R<List<InnerUserDTO>> getAllUserInfo(@RequestBody List<Long> ids);
}
