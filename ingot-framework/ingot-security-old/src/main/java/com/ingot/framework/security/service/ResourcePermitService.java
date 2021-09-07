package com.ingot.framework.security.service;

import java.util.List;

/**
 * <p>Description  : ResourcePermitService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/28.</p>
 * <p>Time         : 6:48 下午.</p>
 */
public interface ResourcePermitService {

    /**
     * Resource permit ant patterns
     */
    List<String> allResourcePermitAntPatterns();

    /**
     * Resource permit
     *
     * @param requestURI check url
     */
    boolean resourcePermit(String requestURI);

    /**
     * 是否过滤 {@link com.ingot.framework.security.provider.filter.UserAuthenticationFilter}
     *
     * @param requestURI check url
     */
    boolean userPermit(String requestURI);
}
