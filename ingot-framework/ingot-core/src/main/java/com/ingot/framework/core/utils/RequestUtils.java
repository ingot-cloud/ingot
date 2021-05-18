package com.ingot.framework.core.utils;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.common.constants.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * <p>Description  : RequestUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/4.</p>
 * <p>Time         : 上午10:25.</p>
 */
@Slf4j
public final class RequestUtils {

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
            log.debug(">>> RequestUtils - RequestUtils.getRequest() error, message={}, e={}",
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
                    log.error("RequestUtils servlet - 获取IP地址, 出现异常={}", e.getMessage(), e);
                }
                assert inet != null;
                ipAddress = inet.getHostAddress();
            }
            log.info("RequestUtils servlet - 获取IP地址 ipAddress={}", ipAddress);
        }
        // 对于通过多个代理的情况, 第一个IP为客户端真实IP,多个IP按照','分割 //"***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > GlobalConstants.MAX_IP_LENGTH) {
            if (ipAddress.indexOf(GlobalConstants.COMMA) > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(GlobalConstants.COMMA));
            }
        }
        return ipAddress;
    }

}
