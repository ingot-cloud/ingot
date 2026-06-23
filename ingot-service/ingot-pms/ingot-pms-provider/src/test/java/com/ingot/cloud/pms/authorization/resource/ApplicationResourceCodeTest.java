package com.ingot.cloud.pms.authorization.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.authorization.engine.PermissionMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * 应用权限编码构建与匹配规则回归。
 */
class ApplicationResourceCodeTest {

    @Test
    void antSubtreeRootBuildsNamespaceCode() {
        PlatformPermission root = new PlatformPermission();
        root.setCode("contacts:**");

        String childCode = invokeBuildChildCode(root, "user:*", "contacts");
        assertEquals("contacts:user:*", childCode);
        assertTrue(PermissionMatcher.matches("contacts:**", "contacts:user:create"));
        assertTrue(PermissionMatcher.matches(childCode, "contacts:user:create"));
    }

    @Test
    void directorySuffixShouldUseAntSubtree() {
        assertTrue(PermissionMatcher.isAntSubtreeWildcard("contacts:system:**"));
        assertTrue(PermissionMatcher.matches("contacts:system:**", "contacts:system:user:query"));
    }

    @Test
    void exactParentAppendsSegment() {
        PlatformPermission parent = new PlatformPermission();
        parent.setCode("contacts:user:query");

        String childCode = invokeBuildChildCode(parent, "export", "contacts");
        assertEquals("contacts:user:query:export", childCode);
    }

    @Test
    void promoteAndDemoteCodesAreInverse() {
        // 升级：精确码 -> :**
        assertEquals("a:b:**", invokeAppendAntSubtreeSuffix("a:b"));
        // 幂等：已是 :** 不重复追加
        assertEquals("a:b:**", invokeAppendAntSubtreeSuffix("a:b:**"));
        // 降级：:** -> 精确码，与升级互逆
        assertEquals("a:b", PermissionMatcher.wildcardPathPrefix("a:b:**"));
        assertEquals("a:b", PermissionMatcher.wildcardPathPrefix(invokeAppendAntSubtreeSuffix("a:b")));
        // 升级后父码可覆盖子权限
        assertTrue(PermissionMatcher.matches(invokeAppendAntSubtreeSuffix("a:b"), "a:b:add"));
        Assertions.assertFalse(PermissionMatcher.matches("a:b", "a:b:add"));
    }

    private String invokeAppendAntSubtreeSuffix(String code) {
        ApplicationResourceServiceImpl service = new ApplicationResourceServiceImpl(
                null, null, null, null, null, null, null, null);
        try {
            var method = ApplicationResourceServiceImpl.class.getDeclaredMethod(
                    "appendAntSubtreeSuffix", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, code);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private String invokeBuildChildCode(PlatformPermission parent, String code, String appCode) {
        ApplicationResourceServiceImpl service = new ApplicationResourceServiceImpl(
                null, null, null, null, null, null, null, null);
        try {
            var method = ApplicationResourceServiceImpl.class.getDeclaredMethod(
                    "buildChildCode", PlatformPermission.class, String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, parent, code, appCode);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
