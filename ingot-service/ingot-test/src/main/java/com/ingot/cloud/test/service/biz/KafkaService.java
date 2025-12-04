package com.ingot.cloud.test.service.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : KafkaService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/4.</p>
 * <p>Time         : 13:16.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {
    private static final String TOPIC = "test.tp.v1";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String payload) {
        kafkaTemplate.send(TOPIC, payload);
    }

    @KafkaListener(topics = TOPIC, groupId = "test-group")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            log.info("KafkaService.listen: {}", record.value());
            ack.acknowledge();   // 手动提交 offset
        } catch (Exception e) {
            // 可写入失败 topic
            log.error("KafkaService.listen error", e);
            ack.acknowledge();
        }
    }
}
