package com.ingot.framework.security.core.authority;

import org.springframework.security.core.GrantedAuthority;

/**
 * <p>Description  : {@link GrantedAuthority} 扩展.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 1:52 PM.</p>
 */
public interface InGrantedAuthority<T> extends GrantedAuthority {

    /**
     * 提取实例化时传入值
     *
     * @return 实例化值
     */
    T extract();
}
