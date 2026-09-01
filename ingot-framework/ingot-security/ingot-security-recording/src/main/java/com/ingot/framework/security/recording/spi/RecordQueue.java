package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.EnqueueResult;

import java.time.Duration;
import java.util.List;

/**
 * <p>分级可靠队列 SPI，BEST_EFFORT 内存队列与 DURABLE file spool 共用契约。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param <T> 队列元素类型
 */
public interface RecordQueue<T> {

    /**
     * 将记录接纳到队列，并返回同步接纳结果。
     *
     * @param record 待接纳记录
     * @return 接纳成功或拒绝原因
     */
    EnqueueResult enqueue(T record);

    /**
     * 领取不超过指定数量的当前可消费记录；无可领取记录时最多等待指定时长。
     *
     * <p>记录到达或线程中断时可以提前返回。实现捕获中断后必须恢复当前线程的中断标记；
     * {@code limit <= 0} 时返回空列表，非正等待时间仅执行一次非阻塞领取。</p>
     *
     * @param limit 单次领取数量上限
     * @param wait 无可领取记录时的最长等待时间，不可为 {@code null}
     * @return 已领取记录；超时、中断或数量上限非正时返回空列表
     */
    List<ClaimedRecord<T>> claim(int limit, Duration wait);

    /**
     * 确认指定 claim 已处理完成，队列可推进消费位点并回收相关资源。
     *
     * @param claimIds 已成功处理的 claim 标识
     */
    void ack(List<String> claimIds);

    /**
     * 将指定 claim 标记为处理失败，由实现决定丢弃或按退避策略重新入队。
     *
     * @param claimIds 处理失败的 claim 标识
     * @param cause 失败原因
     */
    void nack(List<String> claimIds, Throwable cause);
}
