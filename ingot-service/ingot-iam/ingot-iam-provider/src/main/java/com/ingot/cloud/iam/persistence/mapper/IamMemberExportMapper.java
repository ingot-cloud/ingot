package com.ingot.cloud.iam.persistence.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamMemberExportEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>映射 iam_member_export，由 Repository 显式绑定租户，不应用旧租户拦截器。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，查询与更新必须绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamMemberExportMapper extends BaseMapper<IamMemberExportEntity> {
}
