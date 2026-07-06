package org.springframework.security.web.context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>Description  : SecurityContextRevokeRepository.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/12.</p>
 * <p>Time         : 11:08 AM.</p>
 */
public interface SecurityContextRevokeRepository {

    /**
     * Revoke the security context on completion of a request.
     *
     * @param request {@link HttpServletRequest}
     */
    void revokeContext(HttpServletRequest request);
}
