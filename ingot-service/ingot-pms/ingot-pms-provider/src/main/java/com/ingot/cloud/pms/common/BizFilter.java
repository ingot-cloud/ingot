package com.ingot.cloud.pms.common;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.dto.menu.MenuFilterDTO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;

import java.util.function.Predicate;

/**
 * <p>Description  : BizFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/26.</p>
 * <p>Time         : 6:59 PM.</p>
 */
public final class BizFilter {

    /**
     * 菜单过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<MenuTreeNodeVO> menuFilter(MenuFilterDTO condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            if (StrUtil.isNotEmpty(condition.getOrgTypeText())) {
                return StrUtil.equals(item.getOrgType().getValue(), condition.getOrgTypeText());
            }
            return true;
        };
    }

    /**
     * 权限过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<SysAuthority> authorityFilter(AuthorityFilterDTO condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            if (StrUtil.isNotEmpty(condition.getOrgTypeText())) {
                return StrUtil.equals(item.getType().getValue(), condition.getOrgTypeText());
            }
            return true;
        };
    }

    /**
     * 部门过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<SysDept> deptFilter(SysDept condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getName())) {
                return StrUtil.startWith(item.getName(), condition.getName());
            }
            return true;
        };
    }

    /**
     * 客户端过滤器
     *
     * @param condition 条件
     * @return {@link Predicate}
     */
    public static Predicate<Oauth2RegisteredClient> clientFilter(Oauth2RegisteredClient condition) {
        return (item) -> {
            if (condition == null) {
                return true;
            }
            if (StrUtil.isNotEmpty(condition.getClientId())) {
                return StrUtil.startWith(item.getClientId(), condition.getClientId());
            }
            return true;
        };
    }
}
