package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.api.model.domain.Member;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Mapper
public interface AppRoleUserMapper extends BaseMapper<AppRoleUser> {

    /**
     * 创建用户角色关系，如果已存在则忽略
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("userId") long userId);

    /**
     * 获取角色用户
     *
     * @param page   分页参数
     * @param roleId 角色ID
     * @param isBind 是否绑定
     * @return 分页信息
     */
    IPage<Member> getRoleUsers(Page<?> page,
                               @Param("roleId") long roleId,
                               @Param("isBind") boolean isBind,
                               @Param("condition") Member condition);

}
