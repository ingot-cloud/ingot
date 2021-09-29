package com.ingot.cloud.pms.rest.inner;

import com.ingot.cloud.pms.service.biz.UserDetailService;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.core.userdetails.UserDetailsRequest;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
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

    @Permit(mode = PermitMode.INNER)
    @PostMapping
    public R<UserDetailsResponse> getUserAuthDetail(@RequestBody UserDetailsRequest params) {
        return ok(userDetailService.getUserAuthDetails(params));
    }
}
