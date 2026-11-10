package com.ingot.cloud.iam.application.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.catalog.ActionMatchMode;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.catalog.MenuAccessMode;
import com.ingot.cloud.iam.domain.catalog.MenuDraft;
import com.ingot.cloud.iam.domain.catalog.MenuKind;
import com.ingot.cloud.iam.domain.catalog.MenuRouteParam;

/**
 * <p>统一菜单创建、整包和更新的高级配置默认值、保留语义及参数校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MenuConfiguration {
    private static final Pattern PARAMETER_NAME = Pattern.compile(MenuRouteParam.NAME_PATTERN);
    private static final TypeReference<List<MenuRouteParam>> PARAMETERS = new TypeReference<>() { };
    private static final String DYNAMIC_MARKER = ":";
    private static final String PATH_ROOT = "/";
    private static final String QUERY_MARKER = "?";
    private static final String HASH_MARKER = "#";

    private MenuConfiguration() {
    }

    /**
     * 读取参数声明，存量空列返回空列表。
     * @param row 菜单实体
     * @return 有序声明
     */
    public static List<MenuRouteParam> parameters(IamMenuEntity row) {
        List<MenuRouteParam> result = IamJsonCodec.read(row.getRouteParams(), PARAMETERS);
        return result == null ? List.of() : List.copyOf(result);
    }

    /**
     * 创建取默认值，更新取遗漏字段原值，显式关闭透传清除声明。
     * @param draft 请求内容
     * @param current 更新的锁定记录；创建为空
     * @return 已规范化且通过校验的内容
     */
    public static MenuDraft normalize(MenuDraft draft, IamMenuEntity current) {
        boolean hidden = value(draft.hidden(), current == null ? null : current.getHidden());
        boolean cache = value(draft.isCache(), current == null ? null : current.getIsCache());
        boolean props = value(draft.props(), current == null ? null : current.getProps());
        List<MenuRouteParam> params = draft.routeParams() != null ? draft.routeParams()
                : current == null ? List.of() : parameters(current);
        if (Boolean.FALSE.equals(draft.props())) {
            params = List.of();
        }
        boolean directory = draft.kind() == MenuKind.DIRECTORY;
        if (directory && (cache || props || !params.isEmpty())) {
            throw invalid("目录菜单不支持页面缓存或路由参数透传");
        }
        if (props) {
            if (params.isEmpty() || !hidden || draft.path() == null || draft.path().isBlank()
                    || !draft.path().startsWith(PATH_ROOT) || draft.path().contains(DYNAMIC_MARKER)
                    || draft.path().contains(QUERY_MARKER) || draft.path().contains(HASH_MARKER)) {
                throw invalid("路由参数透传仅支持隐藏菜单，需填写静态站内路径并至少配置一个参数");
            }
        } else if (!params.isEmpty()) {
            throw invalid("请开启路由参数透传，或清空菜单的路由参数");
        }
        Set<String> names = new HashSet<>();
        for (MenuRouteParam param : params) {
            if (param == null || param.name() == null || !PARAMETER_NAME.matcher(param.name()).matches()
                    || !names.add(param.name())) {
                throw invalid("菜单路由参数名称格式不正确或重复");
            }
        }
        List<String> actions = draft.actionIds() == null ? List.of() : draft.actionIds();
        if (!directory && draft.accessMode() == MenuAccessMode.ACTION && actions.isEmpty()) {
            throw invalid("按权限开放的菜单至少需要绑定一个操作");
        }
        if ((directory || draft.accessMode() == MenuAccessMode.OPEN) && !actions.isEmpty()) {
            throw invalid("目录菜单或直接开放的菜单不能绑定权限操作");
        }
        return new MenuDraft(draft.parentId(), draft.name(), draft.kind(), draft.path(), draft.viewPath(),
                draft.routeName(), draft.icon(), directory ? MenuAccessMode.OPEN : draft.accessMode(),
                directory ? ActionMatchMode.ANY : draft.matchMode(), actions, draft.sortOrder(), hidden, cache,
                props, params);
    }

    private static boolean value(Boolean requested, Boolean previous) {
        return Boolean.TRUE.equals(requested == null ? previous : requested);
    }

    private static BizException invalid(String message) {
        return new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), message);
    }
}
