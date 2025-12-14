package com.ingot.cloud.auth.common;

import java.util.function.Predicate;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.auth.model.domain.Oauth2RegisteredClient;

/**
 * <p>Description  : BizFilter.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/14.</p>
 * <p>Time         : 15:14.</p>
 */
public class BizFilter {
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
