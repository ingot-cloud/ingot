package com.ingot.cloud.pms.authorization.engine;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionMatchModeEnum;

/**
 * <p>菜单可见性判定：目录靠祖先补齐，开放页免鉴权，受保护页按 ANY/ALL 匹配具体权限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MenuVisibility {

    private MenuVisibility() {
    }

    /**
     * 判断菜单节点自身是否可见（目录恒为不可见，由祖先补齐）。
     *
     * @param menuType      菜单类型
     * @param accessMode    访问模式
     * @param matchMode     ANY/ALL，空则按 ANY
     * @param requiredCodes 可见性关联的具体权限码；受保护页为空则不可见
     * @param authorization 用户有效授权
     * @return 节点自身可见时返回 {@code true}
     */
    public static boolean visible(MenuTypeEnum menuType,
                                  AccessModeEnum accessMode,
                                  PermissionMatchModeEnum matchMode,
                                  List<String> requiredCodes,
                                  EffectiveAuthorization authorization) {
        if (menuType == MenuTypeEnum.Directory || menuType == MenuTypeEnum.Button) {
            return false;
        }
        if (accessMode == AccessModeEnum.OPEN) {
            return true;
        }
        if (authorization == null) {
            return false;
        }
        List<String> codes = CollUtil.emptyIfNull(requiredCodes).stream()
                .filter(StrUtil::isNotBlank)
                .toList();
        if (codes.isEmpty()) {
            return false;
        }
        if (matchMode == PermissionMatchModeEnum.ALL) {
            return codes.stream().allMatch(authorization::hasPermission);
        }
        return codes.stream().anyMatch(authorization::hasPermission);
    }
}
