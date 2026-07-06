package com.ingot.framework.security.oauth2.server.authorization;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

/**
 * 登录信息提取器
 * 从 HttpServletRequest 中提取客户端信息（IP、User-Agent、设备类型、操作系统、浏览器等）
 *
 * <p>Author: wangchao</p>
 * <p>Date: 2025-12-17</p>
 */
@Slf4j
public class LoginInfoExtractor {

    private static final UserAgentAnalyzer USER_AGENT_ANALYZER = UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(10000)
            .build();

    /**
     * 从请求中提取登录信息
     *
     * @param request HTTP 请求（可选，如果为 null 则从 Spring RequestContextHolder 获取）
     * @return 登录信息
     */
    public static LoginInfo extract(HttpServletRequest request) {
        // 如果没有传入 request，尝试从 Spring RequestContextHolder 获取
        if (request == null) {
            request = RequestContextHolder.getRequest().orElse(null);
        }
        
        if (request == null) {
            return LoginInfo.builder().build();
        }

        try {
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            LoginInfo.LoginInfoBuilder builder = LoginInfo.builder()
                    .ipAddress(ipAddress)
                    .userAgent(userAgent);

            // 解析 User-Agent
            if (StrUtil.isNotEmpty(userAgent)) {
                parseUserAgent(userAgent, builder);
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("[LoginInfoExtractor] Failed to extract login info", e);
            return LoginInfo.builder().build();
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private static String getClientIp(HttpServletRequest request) {
        // 优先从代理头中获取
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (StrUtil.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 都获取不到则使用 RemoteAddr
        return request.getRemoteAddr();
    }

    /**
     * 解析 User-Agent
     */
    private static void parseUserAgent(String userAgentString, LoginInfo.LoginInfoBuilder builder) {
        try {
            UserAgent userAgent = USER_AGENT_ANALYZER.parse(userAgentString);

            // 设备类型
            String deviceClass = userAgent.getValue(UserAgent.DEVICE_CLASS);
            builder.deviceType(mapDeviceType(deviceClass));

            // 操作系统
            String osName = userAgent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
            String osVersion = userAgent.getValue(UserAgent.OPERATING_SYSTEM_VERSION);
            if (StrUtil.isNotEmpty(osName)) {
                builder.os(osVersion != null ? osName + " " + osVersion : osName);
            }

            // 浏览器
            String browserName = userAgent.getValue(UserAgent.AGENT_NAME);
            String browserVersion = userAgent.getValue(UserAgent.AGENT_VERSION);
            if (StrUtil.isNotEmpty(browserName)) {
                builder.browser(browserVersion != null ? browserName + " " + browserVersion : browserName);
            }

        } catch (Exception e) {
            log.debug("[LoginInfoExtractor] Failed to parse user agent: {}", userAgentString, e);
        }
    }

    /**
     * 映射设备类型
     */
    private static String mapDeviceType(String deviceClass) {
        if (deviceClass == null) {
            return "Unknown";
        }

        return switch (deviceClass) {
            case "Desktop", "Laptop" -> "PC";
            case "Phone" -> "Mobile";
            case "Tablet" -> "Tablet";
            case "Mobile" -> "Mobile";
            case "TV", "Game Console" -> "Other";
            case "Robot", "Robot Mobile" -> "Bot";
            default -> deviceClass;
        };
    }

    /**
     * 登录信息
     */
    @Data
    @Builder
    public static class LoginInfo {
        /**
         * IP 地址
         */
        private String ipAddress;

        /**
         * User-Agent
         */
        private String userAgent;

        /**
         * 设备类型（PC、Mobile、Tablet、Bot、Other、Unknown）
         */
        private String deviceType;

        /**
         * 操作系统
         */
        private String os;

        /**
         * 浏览器
         */
        private String browser;

        /**
         * 地理位置（需要集成IP地址库）
         */
        private String location;
    }
}
