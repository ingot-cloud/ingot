package com.ingot.cloud.pms.api.model.transform;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserBaseInfoDto;
import com.ingot.cloud.pms.api.model.dto.user.UserDto;
import com.ingot.cloud.pms.api.model.vo.user.UserProfileVo;
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
    SysUser to(UserDto in);

    SysUser to(UserBaseInfoDto in);

    UserProfileVo to(SysUser in);

    UserDetailsResponse toUserDetails(SysUser in);
}
