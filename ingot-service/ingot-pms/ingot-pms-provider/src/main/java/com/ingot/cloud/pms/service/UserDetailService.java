package com.ingot.cloud.pms.service;

import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;

/**
 * <p>Description  : UserDetailService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:23 下午.</p>
 */
public interface UserDetailService {

    /**
     * 获取用户详情信息
     *
     * @param params   参数
     * @return {@link UserAuthDetails}
     */
    UserAuthDetails getUserAuthDetails(UserDetailsDto params);
}
