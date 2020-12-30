package com.ingot.cloud.pms.rest.inner;

import com.ingot.cloud.pms.service.UserDetailService;
import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.annotation.Permit;
import com.ingot.framework.security.model.enums.PermitModel;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : UserDetailApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/user/detail")
@AllArgsConstructor
public class UserDetailApi extends BaseController {
    private final UserDetailService userDetailService;

    @Permit(model = PermitModel.INNER)
    @PostMapping
    public IngotResponse<UserAuthDetails> getUserAuthDetail(@RequestBody UserDetailsDto params) {
        // todo 请求参数封装
        // test data
//        log.info("load user detail, tenant id = {}", TenantContextHolder.get());
//        UserAuthDetails result = new UserAuthDetails();
//        result.setId(1L);
//        result.setTenantId(1);
//        result.setDeptId(1L);
//        result.setAuthType("unique");
//        result.setUsername("admin");
//        result.setPassword("{noop}admin");
//        result.setStatus(CommonStatusEnum.ENABLE.getValue());
//        result.setRoles(ListUtil.toList("role_admin"));
        return ok(userDetailService.getUserAuthDetails(TenantContextHolder.get(), params));
//        return ok(result);
    }
}
