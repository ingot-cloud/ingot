package com.ingot.cloud.pms.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.enums.AccessModeEnum;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import org.junit.jupiter.api.Test;

class BizMenuUtilsTest {

    @Test
    void filterMenusShouldAllowOpenMenuWithoutPermission() {
        MenuTreeNodeVO openMenu = menu(1L, 0L, AccessModeEnum.OPEN, 1L, 1);
        List<MenuTreeNodeVO> result = BizMenuUtils.filterMenus(List.of(openMenu), List.of());
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void filterMenusShouldRequirePermissionWhenEnabled() {
        MenuTreeNodeVO protectedMenu = menu(2L, 0L, AccessModeEnum.PERMISSION, 100L, 1);
        PlatformPermission granted = permission(100L, "org:contacts:user", CommonStatusEnum.ENABLE);
        List<MenuTreeNodeVO> result = BizMenuUtils.filterMenus(List.of(protectedMenu), List.of(granted));
        assertEquals(1, result.size());

        List<MenuTreeNodeVO> denied = BizMenuUtils.filterMenus(List.of(protectedMenu), List.of());
        assertTrue(denied.isEmpty());
    }

    @Test
    void filterMenusShouldAppendEnabledAncestor() {
        MenuTreeNodeVO parent = menu(10L, 0L, AccessModeEnum.PERMISSION, 1000L, 1);
        parent.setStatus(CommonStatusEnum.ENABLE);
        MenuTreeNodeVO child = menu(11L, 10L, AccessModeEnum.PERMISSION, 1001L, 2);
        List<MenuTreeNodeVO> all = List.of(parent, child);
        PlatformPermission childPermission = permission(1001L, "org:contacts:dept", CommonStatusEnum.ENABLE);

        List<MenuTreeNodeVO> result = BizMenuUtils.filterMenus(all, List.of(childPermission));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(10L)));
        assertTrue(result.stream().anyMatch(item -> item.getId().equals(11L)));
    }

    @Test
    void getMenuAuthorityCodeShouldConvertPath() {
        var menu = new PlatformMenu();
        menu.setPath("/org/contacts/user");
        assertEquals("org:contacts:user", BizMenuUtils.getMenuAuthorityCode(menu));
    }

    private MenuTreeNodeVO menu(Long id, Long pid, AccessModeEnum accessMode, Long permissionId, int sort) {
        MenuTreeNodeVO node = new MenuTreeNodeVO();
        node.setId(id);
        node.setPid(pid);
        node.setAccessMode(accessMode);
        node.setPermissionId(permissionId);
        node.setSort(sort);
        node.setStatus(CommonStatusEnum.ENABLE);
        return node;
    }

    private PlatformPermission permission(Long id, String code, CommonStatusEnum status) {
        PlatformPermission permission = new PlatformPermission();
        permission.setId(id);
        permission.setCode(code);
        permission.setStatus(status);
        return permission;
    }
}
