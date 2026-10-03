package com.ingot.cloud.iam.delegation;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRecipientRepository;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>不限单次期限仍受来源的起止边界和禁止自我授权约束。</p>
 * @author jy
 * @since 1.0.0
 */
class DelegationDurationAdmissionTest {
    @Test
    void unlimitedAllowsLongOrBoundedGrantOnlyWhileStartAndEndFitSource() {
        var assignments = mock(AssignmentRepository.class);
        var recipients = mock(DelegationRecipientRepository.class);
        var source = new IamDelegationGrantEntity();
        source.setStatus(GrantStatus.ACTIVE);
        source.setAssignmentDurationMode(AssignmentDurationMode.UNLIMITED);
        source.setPlatformAdministratorId(BigInteger.valueOf(99));
        Instant end = Instant.now().plusSeconds(3600);
        source.setValidUntil(LocalDateTime.ofInstant(end, ZoneOffset.UTC));
        when(assignments.findDelegation(AuthorizationDomain.PLATFORM, null, 60)).thenReturn(source);
        when(assignments.delegationAllowsRevision(60, 31)).thenReturn(true);
        when(recipients.reaches(AuthorizationDomain.PLATFORM, null, 60, 1)).thenReturn(true);
        var admission = new DelegationAdmission(assignments, recipients);
        assertTrue(admission.check(request(Instant.now(), null)).isEmpty());
        assertTrue(admission.check(request(Instant.now(), end.minusSeconds(1))).isEmpty());
        assertFalse(admission.check(request(end, null)).isEmpty());
        assertFalse(admission.check(request(Instant.now(), end.plusSeconds(1))).isEmpty());
        source.setAssignmentDurationMode(AssignmentDurationMode.LIMITED);
        source.setMaxAssignmentDurationSeconds(3600L);
        source.setMaxAssignmentDurationNanos(0);
        assertFalse(admission.check(request(Instant.now(), null)).isEmpty());
    }
    private DelegationAdmission.Request request(Instant from, Instant until) {
        return new DelegationAdmission.Request(AuthorizationDomain.PLATFORM, null, 60, 99L,
                31, List.of(), Map.of(), new SubjectRef(SubjectType.MEMBER, "1"), from, until);
    }
}
