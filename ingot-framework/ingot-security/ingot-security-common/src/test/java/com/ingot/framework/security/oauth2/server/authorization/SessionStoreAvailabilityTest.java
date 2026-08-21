package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SessionStoreAvailability} 宽限期边界与计数。
 *
 * @author jy
 * @since 1.0.0
 */
class SessionStoreAvailabilityTest {

    @Test
    void withinGrace_allowsDegradedPass() {
        SessionStoreAvailability availability = new SessionStoreAvailability(Duration.ofSeconds(30));

        assertTrue(availability.markUnavailableAndAllow());
        assertTrue(availability.markUnavailableAndAllow());
        assertTrue(availability.isDegraded());
        assertEquals(2, availability.getUnavailableCount());
        assertEquals(2, availability.getDegradedPassCount());
    }

    @Test
    void zeroGrace_rejectsImmediately() {
        SessionStoreAvailability availability = new SessionStoreAvailability(Duration.ZERO);

        // 首次失败时 degradedSince 即当前时刻，窗口为 0 仍视为「未超期」，第二次才判失败
        availability.markUnavailableAndAllow();
        sleepAtLeastOneMilli();

        assertFalse(availability.markUnavailableAndAllow());
        assertEquals(2, availability.getUnavailableCount());
        assertEquals(1, availability.getDegradedPassCount());
    }

    @Test
    void markAvailable_resetsGraceWindow() {
        SessionStoreAvailability availability = new SessionStoreAvailability(Duration.ZERO);
        availability.markUnavailableAndAllow();
        sleepAtLeastOneMilli();
        assertFalse(availability.markUnavailableAndAllow());

        availability.markAvailable();
        assertFalse(availability.isDegraded());
        // 恢复后重新计时，新一轮故障回到宽限期内
        assertTrue(availability.markUnavailableAndAllow());
    }

    private void sleepAtLeastOneMilli() {
        long deadline = System.currentTimeMillis() + 1;
        while (System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
    }
}
