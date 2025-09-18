package com.ingot.framework.id.snowflake.impl;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.Preconditions;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.id.IdGenerator;
import com.ingot.framework.id.snowflake.worker.WorkerIdFactory;
import lombok.extern.slf4j.Slf4j;

import static com.ingot.framework.commons.model.status.BaseErrorCode.ID_CLOCK_BACK;

/**
 * <p>Description  : SnowFlakeIdGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/24.</p>
 * <p>Time         : 8:47 下午.</p>
 */
@Slf4j
public class SnowFlakeIdGenerator implements IdGenerator {
    /**
     * 2017-01-18 23:46:01, 起始时间戳，用于用当前时间戳减去这个时间戳，算出偏移量。41位时间位可以使用69年。
     * 起始的时间戳，可以修改为服务第一次启动的时间。
     * 若服务已经开始使用，起始时间戳就不应该改变。
     */
    private static final long START_TIMESTAMP = 1484754361114L;

    /**
     * workerId占用的位数：10
     */
    private static final long WORKER_ID_BITS = 10L;

    /**
     * 最大能够分配的workerId = 1023
     */
    public static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // -1L ^ (-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_BITS = 12L;
    private static final long WORKER_ID_LEFT_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS); // -1L ^ (-1L << SEQUENCE_BITS);
    private static final Random RANDOM = new Random();


    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowFlakeIdGenerator(WorkerIdFactory factory) {
        boolean initFlag = factory.init();
        if (initFlag) {
            workerId = factory.getWorkerId();
            log.info("[SnowFlakeIdGenerator] START SUCCESS USE WORKER-ID={}", workerId);
        }
        Preconditions.checkArgument(initFlag, "Snowflake Id Gen is not init ok");
        Preconditions.checkArgument(workerId >= 0 && workerId <= MAX_WORKER_ID,
                "workerID must gte 0 and lte 1023");
    }

    @Override
    public long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            // 如果大于允许时间回拨的毫秒量，那么抛出异常
            if (offset > 5) {
                throw new BizException(ID_CLOCK_BACK);
            }

            try {
                // 若在允许时间回拨的毫秒量范围内，则允许等待2倍的偏移量后重新获取
                wait(offset << 1);
            } catch (InterruptedException e) {
                throw new BizException(ID_CLOCK_BACK);
            }

            timestamp = timeGen();
            if (timestamp < lastTimestamp) {
                throw new BizException(ID_CLOCK_BACK);
            }

        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号置为 1 - 3 随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT) |
                (workerId << WORKER_ID_LEFT_SHIFT) |
                sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}
