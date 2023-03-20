package com.ingot.framework.id.snowflake.worker.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

import com.ingot.framework.id.snowflake.impl.SnowFlakeIdGenerator;
import com.ingot.framework.id.properties.IdProperties;
import com.ingot.framework.id.snowflake.worker.AbsWorkerIdFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : MachineWorkerIdFactory.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/26.</p>
 * <p>Time         : 11:46 上午.</p>
 */
@Slf4j
public class MachineWorkerIdFactory extends AbsWorkerIdFactory {

    private int workerID;
    private final IdProperties properties;

    public MachineWorkerIdFactory(IdProperties properties, String serviceName, String port) {
        super(properties.getLocalPathPrefix(), serviceName, port);
        this.properties = properties;
    }

    @Override
    public boolean init() {
        try {
            this.workerID = (int) getWorkerIdWithMac();
            updateLocalWorkerID(this.workerID);
            log.info("[MachineWorkerIdFactory] init with workID={}", workerID);
        } catch (Exception e) {
            log.warn("[MachineWorkerIdFactory] - getDatacenterId: " + e.getMessage());
            try {
                this.workerID = getCacheWorkId();
                log.info("[MachineWorkerIdFactory] init from cache workID={}", workerID);
            } catch (Exception exception) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int getWorkerId() {
        return workerID;
    }

    /**
     * Worker id
     */
    private long getWorkerIdWithMac() throws Exception {
        long id = 0L;
        InetAddress inetAddress = findFirstNonLoopbackAddress();
        try {
            if (inetAddress == null) {
                inetAddress = InetAddress.getLocalHost();
            }
            NetworkInterface network = NetworkInterface.getByInetAddress(inetAddress);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (SnowFlakeIdGenerator.MAX_WORKER_ID + 1);
                }
            }
        } catch (Exception e) {
            log.warn("[MachineWorkerIdFactory] getWorkerIdWithMac: " + e.getMessage());
        }
        return id;
    }

    /**
     * org.springframework.cloud.commons.util包，InetUtils#findFirstNonLoopbackAddress()
     */
    private InetAddress findFirstNonLoopbackAddress() {
        InetAddress result = null;
        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces(); nics
                    .hasMoreElements(); ) {
                NetworkInterface ifc = nics.nextElement();
                if (ifc.isUp()) {
                    log.trace("Testing interface: " + ifc.getDisplayName());
                    if (ifc.getIndex() < lowest || result == null) {
                        lowest = ifc.getIndex();
                    } else if (result != null) {
                        continue;
                    }

                    // @formatter:off
                    if (!ignoreInterface(ifc.getDisplayName())) {
                        for (Enumeration<InetAddress> addrs = ifc
                                .getInetAddresses(); addrs.hasMoreElements(); ) {
                            InetAddress address = addrs.nextElement();
                            if (address instanceof Inet4Address
                                    && !address.isLoopbackAddress()
                                    && isPreferredAddress(address)) {
                                log.trace("Found non-loopback interface: "
                                        + ifc.getDisplayName());
                                result = address;
                            }
                        }
                    }
                    // @formatter:on
                }
            }
        } catch (IOException ex) {
            log.error("Cannot get first non-loopback address", ex);
        }

        if (result != null) {
            return result;
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.warn("Unable to retrieve localhost");
        }

        return null;
    }

    boolean isPreferredAddress(InetAddress address) {

        if (this.properties.isUseOnlySiteLocalInterfaces()) {
            final boolean siteLocalAddress = address.isSiteLocalAddress();
            if (!siteLocalAddress) {
                log.trace("Ignoring address: " + address.getHostAddress());
            }
            return siteLocalAddress;
        }
        final List<String> preferredNetworks = this.properties.getPreferredNetworks();
        if (preferredNetworks.isEmpty()) {
            return true;
        }
        for (String regex : preferredNetworks) {
            final String hostAddress = address.getHostAddress();
            if (hostAddress.matches(regex) || hostAddress.startsWith(regex)) {
                return true;
            }
        }
        log.trace("Ignoring address: " + address.getHostAddress());
        return false;
    }

    boolean ignoreInterface(String interfaceName) {
        for (String regex : this.properties.getIgnoredInterfaces()) {
            if (interfaceName.matches(regex)) {
                log.trace("Ignoring interface: " + interfaceName);
                return true;
            }
        }
        return false;
    }
}
