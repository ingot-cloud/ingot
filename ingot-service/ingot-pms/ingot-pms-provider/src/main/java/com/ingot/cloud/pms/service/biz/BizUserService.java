package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;

/**
 * <p>Description  : 业务用户service.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 6:44 PM.</p>
 */
public interface BizUserService {

    /**
     * 获取用户简介信息
     *
     * @param id 用户ID
     * @return {@link UserProfileVO}
     */
    UserProfileVO getUserProfile(long id);

    /**
     * 更新用户基本信息
     *
     * @param params 基本信息参数
     */
    void updateUserBaseInfo(long id, UserBaseInfoDTO params);
}
