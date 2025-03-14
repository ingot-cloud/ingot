package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
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
public interface AppUserMapper extends BaseMapper<AppUser> {
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
}
