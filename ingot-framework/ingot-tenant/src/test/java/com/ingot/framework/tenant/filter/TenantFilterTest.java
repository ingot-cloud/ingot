package com.ingot.framework.tenant.filter;

import java.util.concurrent.atomic.AtomicReference;

import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>验证缺租户时上下文保持为空，合法租户才会写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantFilterTest {

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void missingHeaderLeavesTenantNull() throws Exception {
        AtomicReference<Long> seen = new AtomicReference<>();
        new TenantFilter().doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> seen.set(TenantContextHolder.get()));
        assertNull(seen.get());
        assertNull(TenantContextHolder.get());
    }

    @Test
    void validHeaderSetsTenantThenClears() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderConstants.TENANT, "10");
        AtomicReference<Long> seen = new AtomicReference<>();
        new TenantFilter().doFilter(request, new MockHttpServletResponse(),
                (request1, response) -> seen.set(TenantContextHolder.get()));
        assertEquals(10L, seen.get());
        assertNull(TenantContextHolder.get());
    }

    @Test
    void invalidHeaderLeavesTenantNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderConstants.TENANT, "abc");
        AtomicReference<Long> seen = new AtomicReference<>();
        new TenantFilter().doFilter(request, new MockHttpServletResponse(),
                (request1, response) -> seen.set(TenantContextHolder.get()));
        assertNull(seen.get());
    }
}
