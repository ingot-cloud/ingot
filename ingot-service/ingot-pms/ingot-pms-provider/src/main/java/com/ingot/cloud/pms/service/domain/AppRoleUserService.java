package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
public interface AppRoleUserService extends BaseService<AppRoleUser> {
    /**
     * 根据用户ID删除相关关联角色
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean removeByUserId(long userId);


    /**
     * 更新用户角色，将用户角色更新为指定角色
     *
     * @param userId 用户ID
     * @param roles  待设置的角色
     */
    void setUserRoles(long userId, List<Long> roles);

    /**
     * 用户绑定角色
     *
     * @param params 关联参数
     */
    void userBindRoles(AssignDTO<Long, Long> params);

    /**
     * 角色绑定用户
     *
     * @param params 关联参数
     */
    void roleBindUsers(AssignDTO<Long, Long> params);

    /**
     * 获取角色用户
     *
     * @param roleId    角色id
     * @param page      分页信息
     * @param isBind    是否绑定
     * @param condition 查询条件
     * @return 分页用户
     */
    IPage<AppUser> getRoleUsers(long roleId,
                                Page<?> page,
                                boolean isBind,
                                AppUser condition);
}
