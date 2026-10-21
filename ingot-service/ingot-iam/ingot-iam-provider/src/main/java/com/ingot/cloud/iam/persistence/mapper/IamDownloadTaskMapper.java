package com.ingot.cloud.iam.persistence.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDownloadTaskEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>映射 iam_download_task，由存储实现显式绑定归属，不应用旧租户拦截器。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 仅 {@code IamDownloadTaskStore} 使用；归属查询必须同时绑定域、租户与成员。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDownloadTaskMapper extends BaseMapper<IamDownloadTaskEntity> {
}
