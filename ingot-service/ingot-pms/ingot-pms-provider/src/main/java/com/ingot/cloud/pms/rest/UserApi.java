package com.ingot.cloud.pms.rest;

import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.utils.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/11.</p>
 * <p>Time         : 6:48 下午.</p>
 */
@RestController
@RequestMapping(value = "/user")
public class UserApi extends BaseController {

    @GetMapping
    public IngotResponse<?> user(){
        return ok(SecurityUtils.getUser());
    }
}
