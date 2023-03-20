package com.ingot.framework.id.snowflake.worker.impl;

import java.net.InetAddress;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.ingot.framework.id.snowflake.worker.AbsWorkerIdFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : RedisWorkerIdFactory.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 9:32 下午.</p>
 */
@Slf4j
public class RedisWorkerIdFactory extends AbsWorkerIdFactory {
    private static final String KEY_PREFIX = "ingot:workID:";
    private static final String KEY_INDEX = "ingot:workID:index";
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
        try {
            if (hasKey(key)) {
                workerID = (int) Optional.ofNullable(redisTemplate.opsForValue().get(key))
                        .orElse(-1);
                Preconditions.checkArgument(workerID != -1);
            } else {
                Long index = redisTemplate.opsForValue().increment(KEY_INDEX, 1L);
                Preconditions.checkArgument(index != null);
                log.info("[RedisWorkerIdFactory] current index={}", index);

                workerID = index.intValue();
                redisTemplate.opsForValue().set(key, workerID);
            }

            // 更新本地缓存
            updateLocalWorkerID(workerID);
            log.info("[RedisWorkerIdFactory] init with workID={}", workerID);
        } catch (Exception e){
            log.error("[RedisWorkerIdFactory] init error={}", e.getMessage());
            try {
                workerID = getCacheWorkId();
                log.info("[RedisWorkerIdFactory] init from cache workID={}", workerID);
            } catch (Exception ignore) {
                return false;
            }
        }
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
            log.warn("[RedisWorkerIdFactory] Get IP warn", ex);
        }
        return ip;
    }

}
