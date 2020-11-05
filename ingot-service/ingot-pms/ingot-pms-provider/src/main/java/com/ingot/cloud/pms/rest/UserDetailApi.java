package com.ingot.cloud.pms.rest;

import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.wrapper.BaseController;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.annotation.Permit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.ingot.framework.security.model.enums.PermitModel.INNER;

/**
 * <p>Description  : UserApi.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/4.</p>
 * <p>Time         : 4:41 下午.</p>
 */
@RestController
@RequestMapping(value = "/user")
public class UserDetailApi extends BaseController {

    @Permit(model = INNER)
    @PostMapping(value = "/detail")
    IngotResponse<UserAuthDetails> getUserAuthDetail(@RequestBody UserDetailsDto params){
        // todo 请求参数封装
        return ok();
    }
}
