package com.ingot.cloud.security.service.admission;

import com.ingot.cloud.security.api.model.enums.SecurityEventAdmissionCode;
import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.RecordPriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityEventAdmissionServiceTest {

    @Test
    void durableFailedIsRetryable() {
        SecurityEventAdmissionService.AdmissionResult result =
                SecurityEventAdmissionService.mapOutcome(PublishOutcome.FAILED, RecordPriority.DURABLE);
        assertThat(result.ok()).isFalse();
        assertThat(result.retryable()).isTrue();
        assertThat(result.code()).isEqualTo(SecurityEventAdmissionCode.ADMISSION_RETRYABLE);
    }

    @Test
    void bestEffortDroppedIsNonRetryable() {
        SecurityEventAdmissionService.AdmissionResult result =
                SecurityEventAdmissionService.mapOutcome(PublishOutcome.DROPPED, RecordPriority.BEST_EFFORT);
        assertThat(result.ok()).isFalse();
        assertThat(result.retryable()).isFalse();
        assertThat(result.code()).isEqualTo(SecurityEventAdmissionCode.ADMISSION_REJECTED);
    }

    @Test
    void acceptedIsOk() {
        SecurityEventAdmissionService.AdmissionResult result =
                SecurityEventAdmissionService.mapOutcome(PublishOutcome.ACCEPTED, RecordPriority.DURABLE);
        assertThat(result.ok()).isTrue();
        assertThat(result.retryable()).isFalse();
    }
}
