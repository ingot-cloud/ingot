package com.ingot.cloud.pms.web.inner;

import com.ingot.cloud.pms.service.biz.UserDetailService;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : UserDetailApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/user/details")
@RequiredArgsConstructor
public class UserDetailApi extends BaseController {
    private final UserDetailService userDetailService;

    @PostMapping("/{username}")
    public R<UserDetailsResponse> getUserAuthDetail(@PathVariable String username) {
        return ok(userDetailService.getUserAuthDetails(username));
    }
}
