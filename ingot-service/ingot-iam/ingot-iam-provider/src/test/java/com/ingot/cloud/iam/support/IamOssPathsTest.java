package com.ingot.cloud.iam.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IamOssPathsTest {

    @Test
    void storeStripsPresignedUrl() {
        assertEquals("ingot/user/avatar/a.png", IamOssPaths.store(
                "https://minio.local:9000/ingot/user/avatar/a.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=3600"));
    }

    @Test
    void storeKeepsObjectPath() {
        assertEquals("ingot/user/avatar/a.png", IamOssPaths.store("ingot/user/avatar/a.png"));
    }

    @Test
    void storeLeavesBlankAndUnparsedLiteral() {
        assertNull(IamOssPaths.store(null));
        assertEquals("", IamOssPaths.store("  "));
        assertEquals("avatar", IamOssPaths.store("avatar"));
    }
}
