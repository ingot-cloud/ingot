package com.ingot.cloud.iam.catalog;

import java.util.List;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证声明参数及目录配置的边界。</p>
 * @author jy
 * @since 1.0.0
 */
class MenuConfigurationTest {
    @Test
    void rejectsInvalidStructuredParameters() {
        for (List<MenuRouteParam> params : List.of(List.<MenuRouteParam>of(),
                List.of(new MenuRouteParam("9a", null)),
                List.of(new MenuRouteParam("a", null), new MenuRouteParam("a", "重复")))) {
            assertThrows(BizException.class, () -> MenuConfiguration.normalize(draft("/orders", true, params), null));
        }
        assertThrows(BizException.class, () -> MenuConfiguration.normalize(
                draft("/orders", false, List.of(new MenuRouteParam("a", null))), null));
        assertThrows(BizException.class, () -> MenuConfiguration.normalize(
                draft("/orders/:old", true, List.of(new MenuRouteParam("a", null))), null));
    }

    @Test
    void defaultsAndDirectoryRules() {
        MenuDraft old = new MenuDraft(null, "页", MenuKind.PAGE, "/old/:id", null, null, null,
                MenuAccessMode.OPEN, ActionMatchMode.ANY, List.of(), 0);
        var normalized = MenuConfiguration.normalize(old, null);
        assertEquals(false, normalized.hidden()); assertEquals(false, normalized.isCache());
        assertEquals(false, normalized.props()); assertEquals(List.of(), normalized.routeParams());
        assertEquals("/old/:id", MenuRoutePaths.resolve(normalized.path(), normalized.routeParams()));
        MenuDraft directory = new MenuDraft(null, "目录", MenuKind.DIRECTORY, "/orders", null, null, null,
                MenuAccessMode.ACTION, ActionMatchMode.ALL, List.of(), 0);
        assertEquals(MenuAccessMode.OPEN, MenuConfiguration.normalize(directory, null).accessMode());
        var cachedDirectory = new MenuDraft(null, "目录", MenuKind.DIRECTORY, "/orders", null, null, null,
                MenuAccessMode.OPEN, ActionMatchMode.ANY, List.of(), 0, false, true, false, List.of());
        assertThrows(BizException.class, () -> MenuConfiguration.normalize(cachedDirectory, null));
        MenuDraft protectedPage = new MenuDraft(null, "页", MenuKind.PAGE, "/orders", null, null, null,
                MenuAccessMode.ACTION, ActionMatchMode.ANY, List.of(), 0);
        assertThrows(BizException.class, () -> MenuConfiguration.normalize(protectedPage, null));
    }

    private static MenuDraft draft(String path, boolean hidden, List<MenuRouteParam> params) {
        return new MenuDraft(null, "详情", MenuKind.PAGE, path, "orders.detail", null, null,
                MenuAccessMode.OPEN, ActionMatchMode.ANY, List.of(), 0, hidden, false, true, params);
    }
}
