package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.dto.user.UserInfoDTO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.security.core.userdetails.InUser;

/**
 * <p>Description  : BizAuthService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 09:11.</p>
 */
public interface BizAuthService {

    /**
     * 通过 {@link InUser} 获取用户信息
     *
     * @param user {@link InUser} 当前登录用户
     * @return {@link UserInfoDTO}
     */
    UserInfoDTO getUserInfo(InUser user);

    /**
     * 获取用户可用菜单
     *
     * @param user {@link InUser}
     * @return {@link MenuTreeNodeVO} List
     */
    List<MenuTreeNodeVO> getUserMenus(InUser user);
}
