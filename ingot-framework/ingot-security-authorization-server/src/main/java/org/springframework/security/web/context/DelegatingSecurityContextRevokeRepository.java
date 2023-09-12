package org.springframework.security.web.context;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * <p>Description  : DelegatingSecurityContextRevokeRepository.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/12.</p>
 * <p>Time         : 12:20 PM.</p>
 */
@RequiredArgsConstructor
public class DelegatingSecurityContextRevokeRepository implements SecurityContextRevokeRepository {
    private final List<SecurityContextRevokeRepository> delegates;

    @Override
    public void revokeContext(HttpServletRequest request) {
        for (SecurityContextRevokeRepository delegate : this.delegates) {
            delegate.revokeContext(request);
        }
    }
}
