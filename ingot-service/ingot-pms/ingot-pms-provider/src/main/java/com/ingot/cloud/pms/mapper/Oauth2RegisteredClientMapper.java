package com.ingot.cloud.pms.mapper;

import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.framework.data.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author jymot
 * @since 2021-09-29
 */
@Mapper
public interface Oauth2RegisteredClientMapper extends BaseMapper<Oauth2RegisteredClient> {

}
