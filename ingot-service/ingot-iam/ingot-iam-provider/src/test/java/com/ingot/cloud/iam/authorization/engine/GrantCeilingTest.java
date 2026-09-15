package com.ingot.cloud.iam.authorization.engine;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantCeilingTest {

    @Test
    void leafSetCannotDelegateFutureWildcard() {
        EffectiveAuthorization grantor = EffectiveAuthorization.builder()
                .exactPermissionCodes(Set.of("contacts:user:query", "contacts:user:create"))
                .wildcardPermissionCodes(Set.of())
                .build();

        assertTrue(GrantCeiling.canGrantCode(grantor, "contacts:user:query"));
        assertFalse(GrantCeiling.canGrantCode(grantor, "contacts:user:**"));
        assertTrue(GrantCeiling.deniedAsFutureWildcard(grantor, "contacts:user:**"));
    }

    @Test
    void coveringWildcardCanDelegateNarrowerWildcard() {
        EffectiveAuthorization grantor = EffectiveAuthorization.builder()
                .exactPermissionCodes(Set.of())
                .wildcardPermissionCodes(Set.of("contacts:**"))
                .build();

        assertTrue(GrantCeiling.canGrantCode(grantor, "contacts:user:**"));
        assertTrue(GrantCeiling.canGrantCode(grantor, "contacts:user:query"));
        assertTrue(GrantCeiling.canGrantCodes(grantor, List.of("contacts:user:**", "contacts:dept:query")));
    }
}
