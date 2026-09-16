package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamMenuEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_menu，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamMenuMapper extends BaseMapper<IamMenuEntity> {
    /**
     * 锁定指定应用下的菜单行，调用方须处于事务中。
     * @param applicationId 所属应用 ID
     * @param id 菜单 ID
     * @return 命中行；不存在时为空
     */
    @Select("SELECT name,version FROM iam_menu WHERE application_id=#{applicationId} AND id=#{id} FOR UPDATE")
    IamMenuEntity lockRow(@Param("applicationId") BigInteger applicationId, @Param("id") BigInteger id);
}
