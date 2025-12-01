package com.ingot.cloud.member.api.model.convert;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * <p>Description  : MemberPermissionConvert.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 11:11.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface MemberPermissionConvert {
    MemberPermissionConvert INSTANCE = Mappers.getMapper(MemberPermissionConvert.class);

    MemberPermissionTreeNodeVO toTreeNode(MemberPermission permission);
}
