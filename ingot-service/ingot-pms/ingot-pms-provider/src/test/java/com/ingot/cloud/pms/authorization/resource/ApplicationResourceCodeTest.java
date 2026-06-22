package com.ingot.cloud.pms.authorization.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.authorization.engine.PermissionMatcher;
import org.junit.jupiter.api.Test;

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
