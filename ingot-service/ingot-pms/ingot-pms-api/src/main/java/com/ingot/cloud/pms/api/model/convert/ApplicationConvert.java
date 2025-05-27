package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationOrgPageItemVO;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * <p>Description  : ApplicationTrans.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/26.</p>
 * <p>Time         : 08:50.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface ApplicationConvert {

    ApplicationConvert INSTANCE = Mappers.getMapper(ApplicationConvert.class);

    ApplicationOrgPageItemVO to(SysApplicationTenant in);
}
