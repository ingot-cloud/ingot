package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.OrgUserDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDTO;
import com.ingot.cloud.pms.api.model.dto.user.UserDTO;
import com.ingot.cloud.pms.api.model.vo.user.OrgUserProfileVO;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVO;
import com.ingot.framework.core.model.transform.CommonTypeTransform;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import org.mapstruct.Mapper;

/**
 * <p>Description  : UserTrans.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/24.</p>
 * <p>Time         : 5:02 下午.</p>
 */
@Mapper(componentModel = "spring", uses = CommonTypeTransform.class)
public interface UserTrans {
    SysUser to(UserDTO in);

    SysUser to(OrgUserDTO in);

    SysUser to(UserBaseInfoDTO in);

    UserBaseInfoDTO toUserBaseInfo(SysUser in);

    UserProfileVO toUserProfile(SysUser in);

    OrgUserProfileVO toOrgUserProfile(SysUser in);

    UserDetailsResponse toUserDetails(SysUser in);

    UserDetailsResponse toUserDetails(AppUser in);
}
