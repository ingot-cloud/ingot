package com.ingot.cloud.pms.service.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;

/**
 * <p>Description  : BizAppUserService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 14:06.</p>
 */
public interface BizAppUserService {
    /**
     * 用户列表分页
     *
     * @param page   分页参数
     * @param filter 条件参数
     * @return {@link AppUser}
     */
    IPage<AppUser> page(Page<AppUser> page, AppUser filter);

    /**
     * 用户列表分页
     *
     * @param page   分页参数
     * @param filter 条件参数
     * @return {@link AppUser}
     */
    IPage<AppUser> pageTenant(Page<AppUser> page, AppUser filter);

    /**
     * 创建用户, 如果手机号已经创建用户那么不处理
     *
     * @param params {@link AppUserCreateDTO}
     * @return {@link AppUser}
     */
    AppUser createIfPhoneNotUsed(AppUserCreateDTO params);

    /**
     * 用户更新操作
     *
     * @param params
     */
    void updateUser(AppUser params);
}
