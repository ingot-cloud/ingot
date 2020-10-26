package com.ingot.id.worker.impl;

import com.ingot.id.worker.AbsWorkerIdFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.InetAddress;
import java.util.Optional;

/**
 * <p>Description  : RedisWorkerIdFactory.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 9:32 下午.</p>
 */
@Slf4j
public class RedisWorkerIdFactory extends AbsWorkerIdFactory {
    private static final String KEY_PREFIX = "INGOT:ID:";
    private static final String KEY_INDEX = "INGOT:ID:INDEX";
    private final RedisTemplate<String, Object> redisTemplate;
    private final String serviceName;
    private final String serviceAddress;
    private int workerID;

    public RedisWorkerIdFactory(String prefix,
                                String serviceName,
                                String port,
                                RedisTemplate<String, Object> redisTemplate) {
        super(prefix, serviceName, port);
        this.redisTemplate = redisTemplate;
        this.serviceName = serviceName;
        this.serviceAddress = getIp() + ":" + port;
    }

    @Override public boolean init() {
        final String key = getKey();
        if (hasKey(key)) {
            workerID = (int) Optional.ofNullable(redisTemplate.opsForValue().get(key))
                    .orElse(-1);
            if (workerID == -1){
                try {
                    workerID = getCacheWorkId();
                } catch (Exception e) {
                    return false;
                }
            }
        } else {
            Long index = redisTemplate.opsForValue().increment(KEY_INDEX, 1L);
            log.info(">>> RedisWorkerIdFactory current index={}", index);
            if (index == null) {
                log.error(">>> RedisWorkerIdFactory init id error，不能找到当前redis中存储的索引值");
                return false;
            }
            workerID = index.intValue();
            redisTemplate.opsForValue().set(key, workerID);
        }

        // 更新本地缓存
        updateLocalWorkerID(workerID);
        log.info(">>> RedisWorkerIdFactory init with workID={}", workerID);

        return true;
    }

    @Override public int getWorkerId() {
        return workerID;
    }

    private String getKey(){
        return KEY_PREFIX + serviceName + ":" + serviceAddress;
    }

    private boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }

    private String getIp() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch(Exception ex) {
            ip = "";
            log.warn("Get IP warn", ex);
        }
        return ip;
    }

}
