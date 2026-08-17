package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.DeliveryResult;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

import java.util.List;

/**
 * <p>安全事件远程传输 SPI，Feign/Kafka 等实现；不承担存储选择语义。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface SecurityEventTransport {

    String transportId();

    DeliveryResult deliverBatch(List<SecurityEventRecord> records);
}
