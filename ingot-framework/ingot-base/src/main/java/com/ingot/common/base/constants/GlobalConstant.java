package com.ingot.common.base.constants;

/**
 * <p>Description  : GlobalConstant.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/4.</p>
 * <p>Time         : 上午11:24.</p>
 */
public interface GlobalConstant {

    String UNKNOWN = "unknown";

    String X_FORWARDED_FOR = "X-Forwarded-For";
    String X_REAL_IP = "X-Real-IP";
    String PROXY_CLIENT_IP = "Proxy-Client-IP";
    String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
    String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

    String LOCALHOST_IP = "127.0.0.1";
    String LOCALHOST_IP_16 = "0:0:0:0:0:0:0:1";
    int MAX_IP_LENGTH = 15;

    String COMMA = ",";
    String SPOT = ".";
    /**
     * The constant UNDER_LINE.
     */
    String UNDER_LINE = "_";
    /**
     * The constant PER_CENT.
     */
    String PER_CENT = "%";
    /**
     * The constant AT.
     */
    String AT = "@";
    /**
     * The constant PIPE.
     */
    String PIPE = "||";
    String SHORT_LINE = "-";
    String SPACE = " ";
    String SLASH = "/";
    String MH = ":";
}
