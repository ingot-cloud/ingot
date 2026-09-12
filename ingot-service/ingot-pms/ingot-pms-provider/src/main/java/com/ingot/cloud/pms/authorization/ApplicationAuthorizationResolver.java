package com.ingot.cloud.pms.authorization;

import java.util.List;

import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.authorization.engine.ApplicationMenuTreeBuilder;
import com.ingot.cloud.pms.authorization.engine.EffectiveAuthorization;
import com.ingot.cloud.pms.authorization.engine.EffectiveAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>应用中心化授权解析器，基于当前租户成员身份计算菜单树。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ApplicationAuthorizationResolver {

    private final EffectiveAuthorizationService effectiveAuthorizationService;
    private final ApplicationMenuTreeBuilder applicationMenuTreeBuilder;

    /**
     * 按用户身份解析可见菜单，不使用调用方传入的角色码代替成员关系。
     *
     * @param userId 当前租户下的用户 ID
     * @return 菜单树
     */
    public List<MenuTreeNodeVO> resolveMenus(long userId) {
        EffectiveAuthorization authorization = effectiveAuthorizationService.resolve(userId);
        return applicationMenuTreeBuilder.build(authorization);
    }
}
