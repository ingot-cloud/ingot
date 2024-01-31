package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserPasswordDTO;
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

    /**
     * 创建用户
     *
     * @param params {@link AppUser}
     */
    void createUser(AppUser params);

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     */
    void removeUserById(long id);

    /**
     * 更新用户
     *
     * @param user {@link AppUser}
     */
    void updateUser(AppUser user);

    /**
     * 用户修改密码
     *
     * @param id     用户ID
     * @param params 参数
     */
    void fixPassword(long id, UserPasswordDTO params);

    /**
     * 检查唯一性
     *
     * @param update  待更新的对象
     * @param current 当前对象
     */
    void checkUserUniqueField(AppUser update, AppUser current);

}
