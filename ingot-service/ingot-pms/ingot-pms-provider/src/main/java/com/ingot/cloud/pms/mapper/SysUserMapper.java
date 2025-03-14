package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserQueryDTO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @param tenantId  租户信息
     * @return {@link IPage}，数据项结构 {@link UserPageItemVO}
     */
    IPage<UserPageItemVO> conditionPageWithTenant(Page<SysUser> page,
                                                  @Param("condition") UserQueryDTO condition,
                                                  @Param("tenantId") Long tenantId);

    /**
     * 获取指定组织指定部门用户信息
     *
     * @param page   分页参数
     * @param deptId 部门ID
     * @param orgId  组织ID
     * @return {@link IPage}，数据项结构 {@link UserPageItemVO}
     */
    IPage<UserPageItemVO> pageByDept(Page<SysUser> page,
                                     @Param("deptId") Long deptId,
                                     @Param("tenantId") Long orgId);

}
