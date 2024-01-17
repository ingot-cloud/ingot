package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.framework.data.mybatis.service.BaseService;
import com.ingot.framework.security.core.userdetails.IngotUser;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
public interface AppUserService extends BaseService<AppUser> {
    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @param tenantId  租户信息
     * @return {@link IPage}，数据项结构 {@link AppUser}
     */
    IPage<AppUser> conditionPageWithTenant(Page<AppUser> page,
                                           @Param("condition") AppUser condition,
                                           @Param("tenantId") Long tenantId);

    /**
     * 通过 {@link IngotUser} 获取用户信息
     *
     * @param user {@link IngotUser} 当前登录用户
     * @return {@link UserInfoDTO}
     */
    UserInfoDTO getUserInfo(IngotUser user);
}
