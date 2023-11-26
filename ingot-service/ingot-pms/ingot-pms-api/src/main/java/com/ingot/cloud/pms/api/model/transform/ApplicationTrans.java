package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.SysApplicationTenant;
import com.ingot.cloud.pms.api.model.vo.application.ApplicationPageItemVO;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : ApplicationTrans.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/26.</p>
 * <p>Time         : 08:50.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface ApplicationTrans {

    ApplicationPageItemVO to(SysApplicationTenant in);
}
