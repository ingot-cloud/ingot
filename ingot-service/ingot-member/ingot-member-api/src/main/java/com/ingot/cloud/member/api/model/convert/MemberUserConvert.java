package com.ingot.cloud.member.api.model.convert;

import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.dto.user.MemberUserBaseInfoDTO;
import com.ingot.cloud.member.api.model.dto.user.MemberUserDTO;
import com.ingot.cloud.member.api.model.vo.user.MemberUserProfileVO;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * <p>Description  : MemberUserConvert.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 13:20.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface MemberUserConvert {
    MemberUserConvert INSTANCE = Mappers.getMapper(MemberUserConvert.class);

    MemberUserProfileVO toProfileVO(MemberUser params);

    MemberUser toEntity(MemberUserBaseInfoDTO params);

    MemberUser toEntity(MemberUserDTO params);

    UserDetailsResponse toUserDetails(MemberUser in);

    MemberUserBaseInfoDTO toUserBaseInfo(MemberUser in);
}
