package com.ingot.cloud.iam.session;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.framework.commons.model.iam.ActionMatchMode;
import com.ingot.framework.commons.model.iam.MenuAccessMode;
import com.ingot.framework.commons.model.iam.MenuKind;
import com.ingot.framework.commons.model.iam.MenuNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证开放菜单仍受应用边界约束，空目录不展示，有子页时保留祖先。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class SessionMenuAssemblerTest {

    @Test
    void openPageOfInaccessibleAppIsHidden() {
        SessionMenuAssembler.MenuRow open = page(1, 9, null, MenuAccessMode.OPEN, ActionMatchMode.ANY);
        List<MenuNode> nodes = SessionMenuAssembler.assemble(List.of(open), Set.of(2L), Set.of(), Map.of());
        assertTrue(nodes.isEmpty());
    }

    @Test
    void openPageOfAccessibleAppIsVisibleWithoutAction() {
        SessionMenuAssembler.MenuRow open = page(1, 2, null, MenuAccessMode.OPEN, ActionMatchMode.ANY);
        List<MenuNode> nodes = SessionMenuAssembler.assemble(List.of(open), Set.of(2L), Set.of(), Map.of());
        assertEquals(1, nodes.size());
        assertEquals("1", nodes.getFirst().id());
        assertEquals(MenuKind.PAGE, nodes.getFirst().kind());
    }

    @Test
    void actionPageRequiresMatchingCodes() {
        SessionMenuAssembler.MenuRow protectedPage = page(3, 2, null, MenuAccessMode.ACTION, ActionMatchMode.ALL);
        List<MenuNode> missing = SessionMenuAssembler.assemble(List.of(protectedPage), Set.of(2L),
                Set.of("iam-tenant:member:read"), Map.of(3L, List.of("iam-tenant:member:read", "iam-tenant:member:update")));
        assertTrue(missing.isEmpty());
        List<MenuNode> granted = SessionMenuAssembler.assemble(List.of(protectedPage), Set.of(2L),
                Set.of("iam-tenant:member:read", "iam-tenant:member:update"),
                Map.of(3L, List.of("iam-tenant:member:read", "iam-tenant:member:update")));
        assertEquals(1, granted.size());
    }

    @Test
    void emptyDirectoryIsHiddenAndAncestorKeptWhenChildPageVisible() {
        SessionMenuAssembler.MenuRow empty = directory(10, 2, null);
        SessionMenuAssembler.MenuRow parent = directory(11, 2, null);
        SessionMenuAssembler.MenuRow child = page(12, 2, 11L, MenuAccessMode.ACTION, ActionMatchMode.ANY);
        List<MenuNode> nodes = SessionMenuAssembler.assemble(List.of(empty, parent, child), Set.of(2L),
                Set.of("iam-tenant:member:read"), Map.of(12L, List.of("iam-tenant:member:read")));
        assertEquals(1, nodes.size());
        assertEquals("11", nodes.getFirst().id());
        assertEquals(MenuKind.DIRECTORY, nodes.getFirst().kind());
        assertEquals(1, nodes.getFirst().children().size());
        assertEquals("12", nodes.getFirst().children().getFirst().id());
    }

    private static SessionMenuAssembler.MenuRow page(long id, long appId, Long parentId, MenuAccessMode access,
                                                     ActionMatchMode match) {
        return new SessionMenuAssembler.MenuRow(id, appId, parentId, "页" + id, MenuKind.PAGE, "/p" + id, null,
                null, null, (int) id, match, access);
    }

    private static SessionMenuAssembler.MenuRow directory(long id, long appId, Long parentId) {
        return new SessionMenuAssembler.MenuRow(id, appId, parentId, "目录" + id, MenuKind.DIRECTORY, null, null,
                null, null, (int) id, ActionMatchMode.ANY, MenuAccessMode.OPEN);
    }
}
