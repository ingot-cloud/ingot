package com.ingot.cloud.pms.mapper;

import com.ingot.cloud.pms.model.domain.SysOauthClientDetails;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
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
public interface SysOauthClientDetailsMapper extends BaseMapper<SysOauthClientDetails> {

    /**
     * 根据角色id获取对应可用的client
     *
     * @param roleIds 角色ID
     * @return SysOauthClientDetails 列表
     */
    List<SysOauthClientDetails> getClientsByRoles(@Param("list") List<Long> roleIds);
}
