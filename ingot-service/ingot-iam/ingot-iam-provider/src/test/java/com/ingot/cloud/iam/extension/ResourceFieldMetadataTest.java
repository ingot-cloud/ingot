package com.ingot.cloud.iam.extension;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.persistence.mapper.IamActionMapper;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamResourceMapper;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 验证目录只能收紧注册能力，不能借不兼容默认值扩权。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class ResourceFieldMetadataTest {

    private static final String PHONE = "phone";

    private static final String READ = "iam-ops:incident:read";

    private static final ResourceKey KEY = new ResourceKey(AuthorizationDomain.PLATFORM, "iam-ops", "incident");

    @Test
    void directoryRemovalAndFullOnlyConfigurationCannotPromoteMaskedDefault() {
        var app = new IamApplicationEntity();
        app.setId(BigInteger.ONE);
        app.setDomain(AuthorizationDomain.PLATFORM);
        app.setCode(KEY.applicationCode());
        app.setEnabled(true);
        var row = new IamResourceEntity();
        row.setId(BigInteger.TEN);
        row.setApplicationId(app.getId());
        row.setCode(KEY.resourceCode());
        row.setEnabled(true);
        var provider = mock(ResourceObjectProvider.class);
        when(provider.descriptor()).thenReturn(new ResourceDescriptor(KEY,
                List.of(new ActionDescriptor(READ, ExecutionMode.READ_ONLY)), List.of(ScopeKind.ALL),
                List.of(new FieldCapability(PHONE, "手机号", List.of(FieldVisibility.values()), true, true, com.ingot.framework.commons.model.iam.MaskSpec.PHONE)),
                Map.of(PHONE, new FieldAccess(FieldVisibility.MASKED, false)), READ, false));
        var apps = mock(IamApplicationMapper.class);
        when(apps.selectBatchIds(anyCollection())).thenReturn(List.of(app));
        var service = new ResourceFieldMetadata(apps, mock(IamResourceMapper.class), mock(IamActionMapper.class),
                new ResourceRegistry(List.of(provider)), com.ingot.cloud.iam.persistence.FieldTestSupport.manifests(), com.ingot.cloud.iam.persistence.FieldTestSupport.noCache());
        row.setFieldCapabilities("[]");
        assertEquals(FieldVisibility.HIDDEN,
                service.describe(List.of(row)).get("10").defaults().get(PHONE).visibility());
        row.setFieldCapabilities(
                """
                        [{"key":"phone","label":"手机号","visibilities":["FULL"],"editable":true,"filterable":true}]
                        """);
        var safe = service.describe(List.of(row)).get("10");
        assertEquals(List.of(FieldVisibility.HIDDEN), safe.fields().getFirst().visibilities());
        assertFalse(safe.fields().getFirst().editable());
        row.setFieldCapabilities(
                """
                        [{"key":"phone","label":"手机号","visibilities":["HIDDEN","MASKED","FULL"],"editable":true,"filterable":false,"mask":{"kind":"PHONE"}}]
                        """);
        var supported = service.describe(List.of(row)).get("10");
        assertEquals(FieldVisibility.MASKED, supported.defaults().get(PHONE).visibility());
        assertTrue(supported.fields().getFirst().editable());
        assertFalse(supported.fields().getFirst().filterable());
        app.setEnabled(false);
        assertTrue(service.describe(List.of(row)).isEmpty());
    }

}
