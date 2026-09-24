package com.ingot.cloud.iam.catalog;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>目录删除被拒时拼出可展示原因，错误码仍是 {@link IamReasonCode#OBJECT_IN_USE}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
final class CatalogInUse {
    static final String APPLICATION_PREFIX = "无法删除应用：";
    static final String OWNED_PREFIX = "下仍有";
    static final String REFERENCED_PREFIX = "仍被";
    static final String REFERENCED_SUFFIX = "引用";
    static final String OWNED_AND_REFERENCED = "，且";
    static final String ITEM_SEPARATOR = "、";
    static final String RESOURCES = "资源";
    static final String MENUS = "菜单";
    static final String ENTITLEMENTS = "组织开通";
    static final String PLANS = "套餐";
    static final String RESOURCE_HAS_ACTIONS = "无法删除资源：下仍有操作";
    static final String ACTION_HAS_MENUS = "无法删除操作：仍被菜单引用";
    static final String ACTION_HAS_GRANTS = "无法删除操作：仍被角色授权引用";
    static final String ACTION_HAS_DELTAS = "无法删除操作：仍被角色差异引用";
    static final String MENU_HAS_CHILDREN = "无法删除菜单：下仍有子菜单";

    private CatalogInUse() {
    }

    /**
     * 应用仍有下属目录或外部引用时拒绝删除，并列出全部挡住删除的原因。
     *
     * @param resources 资源数
     * @param menus 菜单数
     * @param entitlements 开通数
     * @param plans 套餐引用数
     */
    static void requireApplicationUnused(long resources, long menus, long entitlements, long plans) {
        List<String> owned = new ArrayList<>();
        addIfPresent(owned, resources, RESOURCES);
        addIfPresent(owned, menus, MENUS);
        List<String> referenced = new ArrayList<>();
        addIfPresent(referenced, entitlements, ENTITLEMENTS);
        addIfPresent(referenced, plans, PLANS);
        if (owned.isEmpty() && referenced.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder(APPLICATION_PREFIX);
        if (!owned.isEmpty()) {
            message.append(OWNED_PREFIX).append(String.join(ITEM_SEPARATOR, owned));
        }
        if (!referenced.isEmpty()) {
            if (!owned.isEmpty()) {
                message.append(OWNED_AND_REFERENCED);
            }
            message.append(REFERENCED_PREFIX).append(String.join(ITEM_SEPARATOR, referenced)).append(REFERENCED_SUFFIX);
        }
        throw new BizException(IamReasonCode.OBJECT_IN_USE.getCode(), message.toString());
    }

    /**
     * 计数大于 0 时按指定说明拒绝删除。
     *
     * @param count 引用数
     * @param message 可展示中文说明
     */
    static void requireUnused(long count, String message) {
        if (count > 0) {
            throw new BizException(IamReasonCode.OBJECT_IN_USE.getCode(), message);
        }
    }

    private static void addIfPresent(List<String> items, long count, String label) {
        if (count > 0) {
            items.add(label);
        }
    }
}
