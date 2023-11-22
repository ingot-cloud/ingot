package com.ingot.cloud.pms.mapper;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleAuthorityMapper extends BaseMapper<SysRoleAuthority> {

    /**
     * 获取角色权限信息
     *
     * @param roleId 角色ID
     * @param isBind 是否绑定
     * @return 分页信息
     */
    List<SysAuthority> getAuthoritiesByRole(@Param("roleId") long roleId);
}
