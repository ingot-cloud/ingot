package com.ingot.cloud.pms.authorization.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionMatcherTest {

    @Test
    void exactPermissionShouldMatchItselfOnly() {
        assertTrue(PermissionMatcher.matches("org:contacts:user", "org:contacts:user"));
        assertFalse(PermissionMatcher.matches("org:contacts:user", "org:contacts:dept"));
    }

    @Test
    void singleWildcardShouldMatchDescendantsOnly() {
        assertTrue(PermissionMatcher.matches("org:contacts:*", "org:contacts:user"));
        assertTrue(PermissionMatcher.matches("org:contacts:*", "org:contacts:user:create"));
        assertFalse(PermissionMatcher.matches("org:contacts:*", "org:contactsx:user"));
    }

    @Test
    void antSubtreeWildcardShouldMatchAllDescendants() {
        assertTrue(PermissionMatcher.matches("contacts:**", "contacts:user"));
        assertTrue(PermissionMatcher.matches("contacts:**", "contacts:user:create"));
        assertTrue(PermissionMatcher.matches("contacts:**", "contacts:system:role:query"));
        assertFalse(PermissionMatcher.matches("contacts:**", "contact:user"));
    }
}
