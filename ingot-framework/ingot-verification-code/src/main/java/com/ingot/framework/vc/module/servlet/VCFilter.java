package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.common.VCVerifyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : VCFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/29.</p>
 * <p>Time         : 5:24 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VCFilter extends OncePerRequestFilter {
    private final VCProviderManager providerManager;
    private final VCVerifyResolver verifyResolver;

    private final List<VCType> typeList = new ArrayList<>();
    private final List<RequestMatcher> requestMatcherList = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();

        // 遍历type
        VCType[] typeArray = VCType.values();
        for (VCType item : typeArray) {
            typeList.add(item);
            requestMatcherList.add(verifyResolver.getMatcher(item));
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        int len = typeList.size();
        for (int i = 0; i < len; i++) {
            RequestMatcher matcher = requestMatcherList.get(i);
            if (!matcher.matches(request)) {
                continue;
            }
            VCType type = typeList.get(i);
            providerManager.validate(type, new ServletWebRequest(request, response));
        }

        filterChain.doFilter(request, response);

    }
}
