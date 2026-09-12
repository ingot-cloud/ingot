package com.ingot.cloud.pms.authorization.engine;

import java.util.List;
import java.util.Set;

import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.enums.PermissionMatchModeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuVisibilityTest {

    @Test
    void openMenuIsVisibleWithoutPermission() {
        EffectiveAuthorization authorization = EffectiveAuthorization.builder()
                .exactPermissionCodes(Set.of())
                .wildcardPermissionCodes(Set.of())
                .build();
        assertTrue(MenuVisibility.visible(MenuTypeEnum.Menu, AccessModeEnum.OPEN,
                PermissionMatchModeEnum.ANY, List.of(), authorization));
    }

    @Test
    void anyAndAllMatchAssociatedPermissions() {
        EffectiveAuthorization authorization = EffectiveAuthorization.builder()
                .exactPermissionCodes(Set.of("contacts:order:query"))
                .wildcardPermissionCodes(Set.of())
                .build();
        assertTrue(MenuVisibility.visible(MenuTypeEnum.Menu, AccessModeEnum.PERMISSION,
                PermissionMatchModeEnum.ANY,
                List.of("contacts:order:query", "contacts:order:approve"),
                authorization));
        assertFalse(MenuVisibility.visible(MenuTypeEnum.Menu, AccessModeEnum.PERMISSION,
                PermissionMatchModeEnum.ALL,
                List.of("contacts:order:query", "contacts:order:approve"),
                authorization));
    }

    @Test
    void directoryAndButtonAreNotSelfVisible() {
        EffectiveAuthorization authorization = EffectiveAuthorization.builder()
                .exactPermissionCodes(Set.of("contacts:order:query"))
                .wildcardPermissionCodes(Set.of())
                .build();
        assertFalse(MenuVisibility.visible(MenuTypeEnum.Directory, AccessModeEnum.OPEN,
                PermissionMatchModeEnum.ANY, List.of(), authorization));
        assertFalse(MenuVisibility.visible(MenuTypeEnum.Button, AccessModeEnum.PERMISSION,
                PermissionMatchModeEnum.ANY, List.of("contacts:order:query"), authorization));
    }

    @Test
    void protectedMenuWithoutAssociationIsHidden() {
        EffectiveAuthorization authorization = EffectiveAuthorization.builder()
                .exactPermissionCodes(Set.of("contacts:order:query"))
                .wildcardPermissionCodes(Set.of("org:contacts:**"))
                .build();
        assertFalse(MenuVisibility.visible(MenuTypeEnum.Menu, AccessModeEnum.PERMISSION,
                PermissionMatchModeEnum.ANY, List.of(), authorization));
    }
}
