package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVO;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 条件查询用户分页信息
     *
     * @param page      分页条件
     * @param condition 筛选条件
     * @return {@link IPage}，数据项结构 {@link UserPageItemVO}
     */
    IPage<UserPageItemVO> conditionPage(Page<SysUser> page,
                                        @Param("condition") UserDTO condition);
}
