package com.ingot.cloud.iam.policy;

import java.util.Map;

import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证空定义体回落到全组织与脱敏基线，以及平台上限缺省不额外收紧。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultPolicyDefinitionsTest {
    @Test
    void emptyDirectoryDefinitionMeansAll() {
        assertEquals(DirectoryDefaultScope.ALL, DefaultPolicyDefinitions.directoryScope(null));
        assertEquals(DirectoryDefaultScope.ALL, DefaultPolicyDefinitions.directoryScope("{}"));
        assertEquals(DirectoryDefaultScope.SELF, DefaultPolicyDefinitions.directoryScope("{\"scope\":\"SELF\"}"));
    }

    @Test
    void emptyFieldDefinitionMasksPhoneAndLeavesCeilingOpen() {
        var baseline = DefaultPolicyDefinitions.fieldAccess("{}");
        assertEquals(FieldVisibility.MASKED, baseline.get(MemberFieldKey.VALUE_PHONE).visibility());
        assertFalse(baseline.get(MemberFieldKey.VALUE_PHONE).editable());
        assertEquals(FieldVisibility.FULL, baseline.get(MemberFieldKey.VALUE_DISPLAY_NAME).visibility());
        var ceiling = DefaultPolicyDefinitions.fieldCeiling("{}");
        assertEquals(FieldVisibility.FULL, ceiling.get(MemberFieldKey.VALUE_PHONE).visibility());
        assertTrue(ceiling.get(MemberFieldKey.VALUE_PHONE).editable());
        var capped = DefaultPolicyDefinitions.fieldCeiling(
                "{\"ceiling\":{\"phone\":{\"visibility\":\"MASKED\",\"editable\":false}}}");
        assertEquals(FieldVisibility.MASKED, capped.get(MemberFieldKey.VALUE_PHONE).visibility());
        assertEquals(FieldVisibility.FULL, capped.get(MemberFieldKey.VALUE_DISPLAY_NAME).visibility());
    }

    @Test
    void stricterPrefersHiddenThenMasked() {
        var hidden = DefaultPolicyDefinitions.stricter(
                new com.ingot.framework.commons.model.iam.FieldAccess(FieldVisibility.FULL, true),
                new com.ingot.framework.commons.model.iam.FieldAccess(FieldVisibility.HIDDEN, false));
        assertEquals(FieldVisibility.HIDDEN, hidden.visibility());
        assertFalse(hidden.editable());
        Map<String, com.ingot.framework.commons.model.iam.FieldAccess> parsed = DefaultPolicyDefinitions.fieldAccess(
                "{\"fields\":{\"phone\":{\"visibility\":\"FULL\",\"editable\":true}}}");
        assertEquals(FieldVisibility.FULL, parsed.get(MemberFieldKey.VALUE_PHONE).visibility());
        assertTrue(parsed.get(MemberFieldKey.VALUE_PHONE).editable());
    }
}
