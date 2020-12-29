package com.ingot.cloud.pms.service.impl;

import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.cloud.pms.service.UserDetailService;
import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.model.enums.UserDetailsModeEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : UserDetailServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:27 下午.</p>
 */
@Service
@AllArgsConstructor
public class UserDetailServiceImpl implements UserDetailService {
    private final SysUserService sysUserService;

    @Override
    public UserAuthDetails getUserAuthDetails(long tenantId, UserDetailsDto params) {
        UserDetailsModeEnum model = params.getMode();
        if (model != null) {
            switch (model) {
                case PASSWORD:
                    return withPasswordMode(tenantId, params);
                case SOCIAL:
                    return withSocialMode(tenantId, params);
            }
        }
        // todo throw 401 异常
        return null;
    }

    private UserAuthDetails withPasswordMode(long tenantId, UserDetailsDto params) {
        return null;
    }

    private UserAuthDetails withSocialMode(long tenantId, UserDetailsDto params) {
        return null;
    }
}
