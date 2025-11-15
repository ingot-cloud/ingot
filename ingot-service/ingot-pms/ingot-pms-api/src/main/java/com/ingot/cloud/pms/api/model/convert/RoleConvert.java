package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.vo.role.RoleItemVO;
import com.ingot.cloud.pms.api.model.vo.role.RolePageItemVO;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * <p>Description  : RoleTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/22.</p>
 * <p>Time         : 4:22 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface RoleConvert {

    RolePageItemVO to(SysRole role);

    RolePageItemVO to(AppRole role);

    RoleItemVO to(MetaRole in);

    @Mapping(target = "value", source = "id")
    @Mapping(target = "label", source = "name")
    Option<Long> option(SysRole role);

    @Mapping(target = "value", source = "id")
    @Mapping(target = "label", source = "name")
    Option<Long> option(AppRole role);
}
