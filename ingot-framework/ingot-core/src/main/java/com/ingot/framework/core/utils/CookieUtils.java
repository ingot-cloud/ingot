package com.ingot.framework.core.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : CookieUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/16.</p>
 * <p>Time         : 10:17 AM.</p>
 */
@Slf4j
public final class CookieUtils {
    public static final String SESSION_ID_NAME = "JSESSIONID";

    /**
     * 设置cookie域，默认为：secingot.com
     */
    public static final String DEFAULT_COOKIE_DOMAIN = ".ingotcloud.top";
    /**
     * 设置默认路径：/，这个路径即该工程下都可以访问该cookie 如果不设置路径，那么只有设置该cookie路径及其子路径可以访问
     */
    public static final String DEFAULT_COOKIE_PATH = "/";
    /**
     * 设置cookie有效期，根据需要自定义[本系统设置为7天]
     */
    public static final int DEFAULT_COOKIE_MAX_AGE = 60 * 60 * 24 * 7;

    /**
     * Sets cookie.
     *
     * @param name     the name
     * @param value    the value
     * @param maxAge   the max age
     * @param response the response
     */
    public static void setCookie(String name, String value, Integer maxAge, boolean httpOnly, boolean secure, HttpServletResponse response) {
        setCookie(name, value, maxAge, null, DEFAULT_COOKIE_PATH, httpOnly, secure, response);
    }

    /**
     * Sets cookie.
     *
     * @param name     the name
     * @param value    the value
     * @param maxAge   the max age
     * @param domain   the domain
     * @param path     the path
     * @param response the response
     */
    public static void setCookie(String name, String value, Integer maxAge, String domain, String path,
                                 boolean httpOnly, boolean secure, HttpServletResponse response) {
        log.info(">>> CookieUtils setCookie - 设置cookie. name={}, value={}. maxAge={}, domain={}, path={}", name, value, maxAge, domain, path);
        Cookie cookie = new Cookie(name, URLEncoder.encode(value, StandardCharsets.UTF_8));

        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        if (StrUtil.isNotEmpty(domain)) {
            cookie.setDomain(domain);
        }
        if (maxAge != null && maxAge > 0) {
            cookie.setMaxAge(ObjectUtil.defaultIfNull(maxAge, DEFAULT_COOKIE_MAX_AGE));
        }
        cookie.setPath(ObjectUtil.defaultIfEmpty(path, DEFAULT_COOKIE_PATH));
        response.addCookie(cookie);
        log.info(">>> CookieUtils setCookie - 设置cookie. [OK]");
    }

    /**
     * 根据Cookie的key得到Cookie的值.
     *
     * @param request the request
     * @param name    the name
     * @return the cookie value
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
        log.info(">>> CookieUtils getCookieValue 获取指定名称的cookie value. name={}", name);
        Cookie cookie = getCookie(request, name);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    /**
     * 根据Cookie的名称得到Cookie对象.
     *
     * @param request the request
     * @param name    the name
     * @return the cookie
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        log.info(">>> CookieUtils 获取指定名称的cookie. name={}", name);
        Cookie[] cookies = request.getCookies();
        if (cookies == null || StrUtil.isBlank(name)) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (StrUtil.equals(cookie.getName(), name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 根据Cookie的key得到Cookie的值.
     *
     * @param request the request
     * @param name    the name
     * @return the cookie value
     */
    public static String getCookieFirstValue(ServerHttpRequest request, String name) {
        HttpCookie cookie = getCookieFirst(request, name);
        if (cookie != null) {
            return cookie.getValue();
        }
        return null;
    }

    /**
     * 根据Cookie的名称得到Cookie对象.
     *
     * @param request the request
     * @param name    the name
     * @return the cookie
     */
    public static HttpCookie getCookieFirst(ServerHttpRequest request, String name) {
        return request.getCookies().getFirst(name);
    }

    /**
     * 删除指定名称的Cookie.
     *
     * @param name     the name
     * @param response the response
     */
    public static void removeCookie(String name, HttpServletResponse response) {
        removeCookie(name, null, null, response);
    }

    /**
     * 删除指定名称Cookie.
     *
     * @param name     the name
     * @param domain   the domain
     * @param path     the path
     * @param response the response
     */
    public static void removeCookie(String name, String domain, String path, HttpServletResponse response) {
        log.info(">>> CookieUtils removeCookie - 删除指定名称的Cookie. key={}", name);
        Cookie cookie = new Cookie(name, null);
        if (StrUtil.isNotEmpty(domain)) {
            cookie.setDomain(domain);
        }
        cookie.setPath(ObjectUtil.defaultIfEmpty(path, DEFAULT_COOKIE_PATH));
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        log.info("removeCookie - 删除指定名称的Cookie. [OK]");
    }
}
