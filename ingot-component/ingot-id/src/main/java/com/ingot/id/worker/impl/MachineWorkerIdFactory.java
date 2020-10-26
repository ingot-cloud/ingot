package com.ingot.id.worker.impl;

import com.ingot.id.worker.AbsWorkerIdFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;

import static com.ingot.id.impl.SnowFlakeIdGenerator.MAX_WORKER_ID;

/**
 * <p>Description  : MachineWorkerIdFactory.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/26.</p>
 * <p>Time         : 11:46 上午.</p>
 */
@Slf4j
public class MachineWorkerIdFactory extends AbsWorkerIdFactory {

    private int workerID;

    public MachineWorkerIdFactory(String prefix, String serviceName, String port) {
        super(prefix, serviceName, port);
    }

    @Override public boolean init() {
        try {
            this.workerID = (int) getWorkerIdWithMac();
            updateLocalWorkerID(this.workerID);
            log.info(">>> MachineWorkerIdFactory init with workID={}", workerID);
        } catch (Exception e) {
            log.warn(">>> MachineWorkerIdFactory - getDatacenterId: " + e.getMessage());
            try {
                this.workerID = getCacheWorkId();
                log.info(">>> MachineWorkerIdFactory init from cache workID={}", workerID);
            } catch (Exception exception) {
                return false;
            }
        }

        return true;
    }

    @Override public int getWorkerId() {
        return workerID;
    }

    /**
     * Worker id
     */
    private long getWorkerIdWithMac() throws Exception {
        long id = 0L;
        InetAddress ip = InetAddress.getLocalHost();
        NetworkInterface network = NetworkInterface.getByInetAddress(ip);
        if (network == null) {
            id = 1L;
        } else {
            byte[] mac = network.getHardwareAddress();
            if (null != mac) {
                id = ((0x000000FF & (long) mac[mac.length - 2]) |
                        (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                id = id % (MAX_WORKER_ID + 1);
            }
        }
        return id;
    }
}
