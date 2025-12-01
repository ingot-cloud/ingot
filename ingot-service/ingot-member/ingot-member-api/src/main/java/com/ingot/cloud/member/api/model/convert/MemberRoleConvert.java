package com.ingot.cloud.member.api.model.convert;

import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.vo.role.MemberRoleTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * <p>Description  : MemberRoleConvert.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 11:06.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface MemberRoleConvert {

    MemberRoleConvert INSTANCE = Mappers.getMapper(MemberRoleConvert.class);

    MemberRoleTreeNodeVO toTreeNode(MemberRole memberRole);
}
