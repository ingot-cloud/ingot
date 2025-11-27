package com.ingot.cloud.pms.api.model.convert;

import com.ingot.cloud.pms.api.model.domain.Member;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.transform.CommonTypeTransform;
import org.mapstruct.Mapper;

/**
 * <p>Description  : UserTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/24.</p>
 * <p>Time         : 5:02 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface UserConvert {
    SysUser to(UserDTO in);

    SysUser to(OrgUserDTO in);

    Member toAppUser(OrgUserDTO in);

    SysUser to(UserBaseInfoDTO in);

    Member toAppUser(UserBaseInfoDTO in);

    Member to(AppUserCreateDTO in);

    UserBaseInfoDTO toUserBaseInfo(SysUser in);

    UserBaseInfoDTO toUserBaseInfo(Member in);

    UserProfileVO toUserProfile(SysUser in);

    UserProfileVO toUserProfile(Member in);

    OrgUserProfileVO toOrgUserProfile(SysUser in);

    OrgUserProfileVO toOrgUserProfile(Member in);

    UserDetailsResponse toUserDetails(SysUser in);

    UserDetailsResponse toUserDetails(Member in);
}
