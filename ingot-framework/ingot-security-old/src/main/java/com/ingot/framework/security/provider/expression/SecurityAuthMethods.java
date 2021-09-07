package com.ingot.framework.security.provider.expression;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * <p>Description  : PermissionService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/12.</p>
 * <p>Time         : 11:07 AM.</p>
 */
@Slf4j
@Service("pms")
public class SecurityAuthMethods {

    /**
     * 判断是否包含角色
     *
     * @param role 角色
     * @return Boolean
     */
    public final boolean hasRole(String role) {
        if (StrUtil.isBlank(role)) {
            return false;
        }
        Authentication authentication = SecurityAuthContext.getAuthentication();
        if (authentication == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .anyMatch(x -> PatternMatchUtils.simpleMatch(role, x));
    }

    /**
     * 判断是否包含任一角色
     *
     * @param roles 角色
     * @return Boolean
     */
    public final boolean hasAnyRole(String... roles) {
        if (ArrayUtil.isEmpty(roles)) {
            return false;
        }
        Authentication authentication = SecurityAuthContext.getAuthentication();
        if (authentication == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> current = Lists.newArrayList(roles);
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .anyMatch(x -> current.stream().anyMatch(c -> PatternMatchUtils.simpleMatch(c, x)));
    }

}
