package com.ingot.cloud.iam.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionMatchMode;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MenuAccessMode;
import com.ingot.framework.commons.model.iam.MenuDraft;
import com.ingot.framework.commons.model.iam.MenuKind;
import com.ingot.framework.commons.model.iam.MenuRouteParam;

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
        List<MenuRouteParam> result = IamJson.read(row.getRouteParams(), PARAMETERS);
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
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (props) {
            if (params.isEmpty() || !hidden || draft.path() == null || draft.path().isBlank()
                    || !draft.path().startsWith(PATH_ROOT) || draft.path().contains(DYNAMIC_MARKER)
                    || draft.path().contains(QUERY_MARKER) || draft.path().contains(HASH_MARKER)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        } else if (!params.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Set<String> names = new HashSet<>();
        for (MenuRouteParam param : params) {
            if (param == null || param.name() == null || !PARAMETER_NAME.matcher(param.name()).matches()
                    || !names.add(param.name())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        List<String> actions = draft.actionIds() == null ? List.of() : draft.actionIds();
        if (!directory && draft.accessMode() == MenuAccessMode.ACTION && actions.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if ((directory || draft.accessMode() == MenuAccessMode.OPEN) && !actions.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return new MenuDraft(draft.parentId(), draft.name(), draft.kind(), draft.path(), draft.viewPath(),
                draft.routeName(), draft.icon(), directory ? MenuAccessMode.OPEN : draft.accessMode(),
                directory ? ActionMatchMode.ANY : draft.matchMode(), actions, draft.sortOrder(), hidden, cache,
                props, params);
    }

    private static boolean value(Boolean requested, Boolean previous) {
        return Boolean.TRUE.equals(requested == null ? previous : requested);
    }
}
