package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.store.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleUserService extends BaseService<SysRoleUser> {

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
    void updateUserRole(long userId, List<Long> roles);

    /**
     * 用户绑定角色
     *
     * @param params 关联参数
     */
    void userBindRoles(RelationDto<Long, Long> params);

    /**
     * 角色绑定用户
     *
     * @param params 关联参数
     */
    void roleBindUsers(RelationDto<Long, Long> params);

    /**
     * 获取角色用户
     *
     * @param roleId 角色id
     * @param page   分页信息
     * @param isBind 是否绑定
     * @return 分页用户
     */
    IPage<SysUser> getRoleUsers(long roleId, Page<?> page, boolean isBind);

}
