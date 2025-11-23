package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.bo.role.BizAssignRoleBO;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
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

    RoleTreeNodeVO to(RoleType in);

    @Mapping(target = "value", source = "id")
    @Mapping(target = "label", source = "name")
    Option<Long> option(RoleType role);

    TenantRoleUserPrivate to(BizAssignRoleBO in);
}
