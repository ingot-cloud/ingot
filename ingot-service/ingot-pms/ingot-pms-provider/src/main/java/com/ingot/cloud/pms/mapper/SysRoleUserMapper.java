package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleUserMapper extends BaseMapper<SysRoleUser> {

    /**
     * 获取角色用户
     *
     * @param page   分页参数
     * @param roleId 角色ID
     * @param isBind 是否绑定
     * @return 分页信息
     */
    IPage<SysUser> getRoleUsers(Page<?> page,
                                @Param("roleId") long roleId,
                                @Param("isBind") boolean isBind,
                                @Param("condition") SysUser condition);

}
