package com.ingot.framework.security.crypto.hybrid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>{@link HybridProtocolVersion} 单元测试。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class HybridProtocolVersionTest {

    @Test
    void h1WireValue() {
        assertEquals("h1", HybridProtocolVersion.H1.wireValue());
    }

    @Test
    void currentIsH1() {
        assertEquals(HybridProtocolVersion.H1, HybridProtocolVersion.current());
    }

    @Test
    void parseKnownVersion() {
        assertEquals(HybridProtocolVersion.H1, HybridProtocolVersion.parse("h1").orElseThrow());
    }

    @Test
    void parseUnknownOrBlankReturnsEmpty() {
        assertTrue(HybridProtocolVersion.parse("h2").isEmpty());
        assertTrue(HybridProtocolVersion.parse("").isEmpty());
        assertTrue(HybridProtocolVersion.parse(null).isEmpty());
    }
}
