package com.ingot.cloud.pms.authorization;

import java.util.List;

import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.authorization.engine.ApplicationMenuTreeBuilder;
import com.ingot.cloud.pms.authorization.engine.EffectiveAuthorization;
import com.ingot.cloud.pms.authorization.engine.EffectiveAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>应用中心化授权解析器，基于有效授权计算用户的菜单树。</p>
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
     * 解析角色编码对应的菜单树。
     *
     * @param roleCodes 角色编码列表
     * @return 菜单树
     */
    public List<MenuTreeNodeVO> resolveMenus(List<String> roleCodes) {
        EffectiveAuthorization authorization = effectiveAuthorizationService.resolve(roleCodes);
        return applicationMenuTreeBuilder.build(authorization);
    }
}
