package com.ingot.id.impl;

import com.google.common.base.Preconditions;
import com.ingot.framework.base.exception.BaseException;
import com.ingot.id.IdGenerator;
import com.ingot.id.worker.WorkerIdFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

import static com.ingot.framework.base.status.BaseStatusCode.ID_CLOCK_BACK;

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
        if (initFlag){
            workerId = factory.getWorkerId();
            log.info(">>> SnowFlakeIdGenerator START SUCCESS USE WORKER-ID={}", workerId);
        }
        Preconditions.checkArgument(initFlag, "Snowflake Id Gen is not init ok");
        Preconditions.checkArgument(workerId >= 0 && workerId <= MAX_WORKER_ID,
                "workerID must gte 0 and lte 1023");
    }

    @Override public long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            // 如果大于允许时间回拨的毫秒量，那么抛出异常
            if (offset > 5) {
                throw new BaseException(ID_CLOCK_BACK);
            }

            try {
                // 若在允许时间回拨的毫秒量范围内，则允许等待2倍的偏移量后重新获取
                wait(offset << 1);
            } catch (InterruptedException e) {
                throw new BaseException(ID_CLOCK_BACK);
            }

            timestamp = timeGen();
            if (timestamp < lastTimestamp) {
                throw new BaseException(ID_CLOCK_BACK);
            }

        }

        // 相同时间，序列自增
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                //seq 为0的时候表示是下一毫秒时间开始对seq做随机
                sequence = RANDOM.nextInt(100);
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            //如果是新的ms开始
            sequence = RANDOM.nextInt(100);
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
