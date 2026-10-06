package com.ingot.example.ops;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>示例业务持久化；生产接入使用本服务数据源。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
public interface IncidentMapper extends BaseMapper<Incident> {

    /**
     * 锁定实际对象。
     * @param id 稳定对象ID
     * @return 持久化归属与原值
     */
    @Select("SELECT id,owner_member_id,title,contact FROM ops_incident WHERE id=#{id} FOR UPDATE")
    Incident lock(@Param("id") String id);

}
