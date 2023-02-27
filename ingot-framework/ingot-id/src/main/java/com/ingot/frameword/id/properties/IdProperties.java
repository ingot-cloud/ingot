package com.ingot.frameword.id.properties;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : IdProperties.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 3:31 下午.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.id")
public class IdProperties {
    private String mode;
    /**
     * 本地缓存WorkerId根目录
     */
    private String localPathPrefix = "/ingot-data/worker-id/";
    /**
     * List of Java regular expressions for network interfaces that will be ignored.
     */
    private List<String> ignoredInterfaces = new ArrayList<>();

    /**
     * Whether to use only interfaces with site local addresses. See
     * {@link InetAddress#isSiteLocalAddress()} for more details.
     */
    private boolean useOnlySiteLocalInterfaces = false;

    /**
     * List of Java regular expressions for network addresses that will be preferred.
     */
    private List<String> preferredNetworks = new ArrayList<>();
}
