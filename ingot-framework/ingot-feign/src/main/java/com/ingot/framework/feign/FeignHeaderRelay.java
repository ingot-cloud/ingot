package com.ingot.framework.feign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.constants.SecurityConstants;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;

/**
 * <p>Feign 请求头转发的共享约定，供 Servlet 与 WebFlux 拦截器复用。</p>
 *
 * <p>下游（如 Auth）必须看到与网关写入时相同的身份头，登录失败 IP/设备封禁才能与
 * {@code BlacklistFilter} 命中同一 Redis key。{@link HeaderConstants#SECURITY_FROM}
 * 不在转发清单中，由 {@link #applyInsideHeader(RequestTemplate)} 统一写成内部调用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class FeignHeaderRelay {

    /**
     * 历史设备头；部分客户端仍发送，与 {@link HeaderConstants#BFF_DEVICE_FINGERPRINT_HEADER} 并存。
     */
    public static final String LEGACY_DEVICE_ID_HEADER = "deviceid";

    /**
     * 允许从入站请求转发到 Feign 的 Header 名（小写，用于匹配）。
     */
    public static final List<String> RELAY_HEADERS;

    static {
        List<String> names = new ArrayList<>();
        names.add(HttpHeaders.AUTHORIZATION);
        names.add(LEGACY_DEVICE_ID_HEADER);
        names.addAll(List.of(HeaderConstants.IDENTITY_PROPAGATION_HEADERS));
        RELAY_HEADERS = names.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
    }

    private FeignHeaderRelay() {
    }

    /**
     * 判断入站 Header 是否应原样转发到 Feign。
     *
     * @param headerName 入站 Header 名，大小写不敏感；{@code null} 视为不转发
     * @return 在 {@link #RELAY_HEADERS} 中则为 {@code true}
     */
    public static boolean shouldRelay(String headerName) {
        return headerName != null && RELAY_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    /**
     * 将 Feign 调用标记为内部请求（写入 {@link SecurityConstants#HEADER_FROM}）。
     *
     * @param template 当前 Feign 请求模板
     */
    public static void applyInsideHeader(RequestTemplate template) {
        template.header(SecurityConstants.HEADER_FROM, SecurityConstants.HEADER_FROM_INSIDE_VALUE);
    }
}
