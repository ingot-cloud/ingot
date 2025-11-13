package com.ingot.cloud.pms.common;

/**
 * <p>Description  : CacheKey.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/27.</p>
 * <p>Time         : 11:47 AM.</p>
 */
public interface CacheKey {

    /**
     * 缓存默认过期时间,单位：秒, 默认缓存1周
     */
    String DefaultExpiredTimeSeconds = "604800";

    String AuthorityListKey = "'list'";
    String AuthorityRoleKey = "'role-' + #roleId";
    String ClientListKey = "'list'";
    String MenuListKey = "'list'";

    /**
     * 公共缓存Key
     */
    String ListKey = "'list'";
    String ItemKey = "'item-' + #id";
    String CodeKey = "'code-' + #code";

    /**
     * 获取指定角色绑定权限缓存key
     *
     * @param roleId 角色ID
     * @return Key
     */
    static String getAuthorityRoleKey(long roleId) {
        return "role-" + roleId;
    }
}
