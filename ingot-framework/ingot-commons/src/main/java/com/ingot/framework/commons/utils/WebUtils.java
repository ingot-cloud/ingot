package com.ingot.framework.commons.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.GlobalConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>Description  : WebUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/4.</p>
 * <p>Time         : 上午10:25.</p>
 */
@Slf4j
public final class WebUtils extends org.springframework.web.util.WebUtils {

    /**
     * Gets request.
     *
     * @return the request
     */
    public static HttpServletRequest getRequest() {
        try {
            return Objects.requireNonNull(
                            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()))
                    .getRequest();
        } catch (Exception e) {
            log.debug("[WebUtils] - RequestUtils.getRequest() error, error={}",
                    e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获得用户远程地址
     *
     * @param request the request
     * @return the string
     */
    public static String getRemoteIP(HttpServletRequest request) {
        String ipAddress = request.getHeader(GlobalConstants.X_REAL_IP);
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader(GlobalConstants.X_FORWARDED_FOR);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader(GlobalConstants.PROXY_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader(GlobalConstants.WL_PROXY_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader(GlobalConstants.HTTP_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader(GlobalConstants.HTTP_X_FORWARDED_FOR);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (GlobalConstants.LOCALHOST_IP.equals(ipAddress) || GlobalConstants.LOCALHOST_IP_16.equals(ipAddress)) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    log.error("[WebUtils] servlet - 获取IP地址, 出现异常={}", e.getMessage(), e);
                }
                assert inet != null;
                ipAddress = inet.getHostAddress();
            }
            log.info("[WebUtils] servlet - 获取IP地址 ipAddress={}", ipAddress);
        }
        // 对于通过多个代理的情况, 第一个IP为客户端真实IP,多个IP按照','分割 //"***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > GlobalConstants.MAX_IP_LENGTH) {
            if (ipAddress.indexOf(GlobalConstants.COMMA) > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(GlobalConstants.COMMA));
            }
        }
        return ipAddress;
    }

    /**
     * 获取客户端IP
     * 由于hutool还没更新6.0，所以没更新HttpServletRequest的包名，所以临时增加该方法
     * <p>
     * 默认检测的Header:
     *
     * <pre>
     * 1、X-Forwarded-For
     * 2、X-Real-IP
     * 3、Proxy-Client-IP
     * 4、WL-Proxy-Client-IP
     * </pre>
     *
     * <p>
     * otherHeaderNames参数用于自定义检测的Header<br>
     * 需要注意的是，使用此方法获取的客户IP地址必须在Http服务器（例如Nginx）中配置头信息，否则容易造成IP伪造。
     * </p>
     *
     * @param request          请求对象{@link HttpServletRequest}
     * @param otherHeaderNames 其他自定义头文件，通常在Http服务器（例如Nginx）中配置
     * @return IP地址
     */
    public static String getClientIP(HttpServletRequest request, String... otherHeaderNames) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        if (ArrayUtil.isNotEmpty(otherHeaderNames)) {
            headers = ArrayUtil.addAll(headers, otherHeaderNames);
        }

        return getClientIPByHeader(request, headers);
    }

    /**
     * 获取客户端IP
     *
     * <p>
     * headerNames参数用于自定义检测的Header<br>
     * 需要注意的是，使用此方法获取的客户IP地址必须在Http服务器（例如Nginx）中配置头信息，否则容易造成IP伪造。
     * </p>
     *
     * @param request     请求对象{@link HttpServletRequest}
     * @param headerNames 自定义头，通常在Http服务器（例如Nginx）中配置
     * @return IP地址
     */
    public static String getClientIPByHeader(HttpServletRequest request, String... headerNames) {
        String ip;
        for (String header : headerNames) {
            ip = request.getHeader(header);
            if (!NetUtil.isUnknown(ip)) {
                return NetUtil.getMultistageReverseProxyIp(ip);
            }
        }

        ip = request.getRemoteAddr();
        return NetUtil.getMultistageReverseProxyIp(ip);
    }

}
