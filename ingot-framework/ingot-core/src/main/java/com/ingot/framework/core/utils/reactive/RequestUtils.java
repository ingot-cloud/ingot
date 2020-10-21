package com.ingot.framework.core.utils.reactive;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.base.constants.GlobalConstants;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;


/**
 * <p>Description  : RequestUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-19.</p>
 * <p>Time         : 09:27.</p>
 */
@Slf4j
@UtilityClass
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class RequestUtils {

    /**
     * 获得用户远程地址
     *
     * @param request the request
     * @return the string
     */
    public String getRemoteIP(ServerRequest request) {
        String ipAddress = getIPFromHeader(request, GlobalConstants.X_REAL_IP);
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = getIPFromHeader(request, GlobalConstants.X_FORWARDED_FOR);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = getIPFromHeader(request, GlobalConstants.PROXY_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = getIPFromHeader(request, GlobalConstants.WL_PROXY_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = getIPFromHeader(request, GlobalConstants.HTTP_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = getIPFromHeader(request, GlobalConstants.HTTP_X_FORWARDED_FOR);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            Optional<InetSocketAddress> address = request.remoteAddress();
            ipAddress = address.isPresent() ? address.get().getHostName() : GlobalConstants.LOCALHOST_IP;
            if (GlobalConstants.LOCALHOST_IP.equals(ipAddress) || GlobalConstants.LOCALHOST_IP_16.equals(ipAddress)) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    log.error("RequestUtils reactive ServerRequest - 获取IP地址, 出现异常={}", e.getMessage(), e);
                }
                assert inet != null;
                ipAddress = inet.getHostAddress();
            }
            log.info("RequestUtils reactive ServerRequest - 获取IP地址 ipAddress={}", ipAddress);
        }
        // 对于通过多个代理的情况, 第一个IP为客户端真实IP,多个IP按照','分割 //"***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.length() > GlobalConstants.MAX_IP_LENGTH) {
            if (ipAddress.indexOf(GlobalConstants.COMMA) > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(GlobalConstants.COMMA));
            }
        }
        return ipAddress;
    }

    private String getIPFromHeader(ServerRequest request, String header) {
        List<String> headers = request.headers().header(header);
        return CollUtil.isEmpty(headers) ? null : headers.get(0);
    }

    /**
     * 获得用户远程地址
     *
     * @param request the request
     * @return the string
     */
    public String getRemoteIP(ServerHttpRequest request) {
        String ipAddress = request.getHeaders().getFirst(GlobalConstants.X_REAL_IP);
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeaders().getFirst(GlobalConstants.X_FORWARDED_FOR);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeaders().getFirst(GlobalConstants.PROXY_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeaders().getFirst(GlobalConstants.WL_PROXY_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeaders().getFirst(GlobalConstants.HTTP_CLIENT_IP);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeaders().getFirst(GlobalConstants.HTTP_X_FORWARDED_FOR);
        }
        if (StrUtil.isEmpty(ipAddress) || GlobalConstants.UNKNOWN.equalsIgnoreCase(ipAddress)) {
            InetSocketAddress address = request.getRemoteAddress();
            ipAddress = address != null ? address.getHostName() : GlobalConstants.LOCALHOST_IP;
            if (GlobalConstants.LOCALHOST_IP.equals(ipAddress) || GlobalConstants.LOCALHOST_IP_16.equals(ipAddress)) {
                //根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    log.error("RequestUtils reactive ServerHttpRequest - 获取IP地址, 出现异常={}", e.getMessage(), e);
                }
                assert inet != null;
                ipAddress = inet.getHostAddress();
            }
            log.info("RequestUtils reactive ServerHttpRequest - 获取IP地址 ipAddress={}", ipAddress);
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
