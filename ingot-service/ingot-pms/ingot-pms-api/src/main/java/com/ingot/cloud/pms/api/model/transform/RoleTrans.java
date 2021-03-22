package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVo;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : RoleTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 4:22 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface RoleTrans {

    RolePageItemVo to(SysRole role);
}
