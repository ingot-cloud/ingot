package com.ingot.cloud.pms.common;

/**
 * <p>Description  : CacheKey.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/27.</p>
 * <p>Time         : 11:47 AM.</p>
 */
public interface CacheKey {

    String AuthorityListKey = "'list'";
    String AuthorityRoleKey = "'role-' + #roleId";
    String AuthorityRoleAllKey = "'role-*'";
    String ClientListKey = "'list'";
    String ClientRoleKey = "'role-' + #roleId";
    String ClientRoleAllKey = "'role-*'";
}
