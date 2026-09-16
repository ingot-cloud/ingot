package com.ingot.framework.security.recording.transport.feign;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.model.enums.SecurityEventAdmissionCode;
import com.ingot.cloud.security.api.rpc.RemoteSecurityEventService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignSecurityEventTransportTest {

    @Test
    void deliverBatchMapsRetryableResponse() {
        RemoteSecurityEventService remote = mock(RemoteSecurityEventService.class);
        when(remote.report(any())).thenReturn(
                R.error(SecurityEventAdmissionCode.ADMISSION_RETRYABLE.getCode(), "full"));
        FeignSecurityEventTransport transport = new FeignSecurityEventTransport(remote);

        var result = transport.deliverBatch(List.of(sampleRecord()));

        assertThat(result.success()).isFalse();
        assertThat(result.retryable()).isTrue();
    }

    @Test
    void deliverBatchSuccess() {
        RemoteSecurityEventService remote = mock(RemoteSecurityEventService.class);
        when(remote.report(any())).thenReturn(R.ok());
        FeignSecurityEventTransport transport = new FeignSecurityEventTransport(remote);

        var result = transport.deliverBatch(List.of(sampleRecord()));

        assertThat(result.success()).isTrue();
        assertThat(result.acceptedCount()).isEqualTo(1);
    }

    private static SecurityEventRecord sampleRecord() {
        return SecurityEventRecord.builder()
                .eventId("01234567890123456789012345678901")
                .eventType("LOGIN_SUCCESS")
                .eventCategory("AUTH")
                .priority(RecordPriority.BEST_EFFORT)
                .occurredAt(Instant.parse("2026-08-04T10:00:00Z"))
                .sourceModule("ingot-iam")
                .build();
    }
}
