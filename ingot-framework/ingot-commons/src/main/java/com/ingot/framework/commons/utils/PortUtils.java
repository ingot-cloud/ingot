package com.ingot.framework.commons.utils;

import java.io.IOException;
import java.net.ServerSocket;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : PortUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-11-14.</p>
 * <p>Time         : 10:42.</p>
 */
@Slf4j
public final class PortUtils {

    /**
     * find available port
     */
    public static int findAvailablePort(int defaultPort) {
        int portTmp = defaultPort;
        while (portTmp < 65535) {
            if (!isPortUsed(portTmp)) {
                return portTmp;
            } else {
                portTmp++;
            }
        }
        while (portTmp > 0) {
            if (!isPortUsed(portTmp)) {
                return portTmp;
            } else {
                portTmp--;
            }
        }
        throw new RuntimeException("no available port.");
    }

    /**
     * check port used
     */
    public static boolean isPortUsed(int port) {
        boolean used;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            used = false;
        } catch (IOException e) {
            log.info(">>> PortUtils - port[{}] is in use.", port);
            used = true;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    log.info("");
                }
            }
        }
        return used;
    }
}
