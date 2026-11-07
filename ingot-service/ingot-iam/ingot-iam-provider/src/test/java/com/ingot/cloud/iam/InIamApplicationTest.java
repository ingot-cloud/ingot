package com.ingot.cloud.iam;

import java.util.Set;
import java.util.stream.Collectors;

import com.ingot.cloud.iam.application.catalog.CatalogFacade;
import com.ingot.cloud.iam.configuration.IamReadRequestConfiguration;
import com.ingot.cloud.iam.infrastructure.persistence.IamMapperConfiguration;
import com.ingot.cloud.iam.interfaces.http.platform.PlatformApplicationAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>确保启动入口位于 IAM 根包，默认组件扫描能够发现各层业务与装配组件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class InIamApplicationTest {

    @Test
    void defaultComponentScanCoversAllIamBeanLayers() {
        assertEquals("com.ingot.cloud.iam", InIamApplication.class.getPackageName());
        assertNotNull(AnnotatedElementUtils.findMergedAnnotation(InIamApplication.class, SpringBootApplication.class));
        ComponentScan scan = AnnotatedElementUtils.findMergedAnnotation(InIamApplication.class, ComponentScan.class);
        assertNotNull(scan);
        assertEquals(0, scan.basePackages().length);
        assertEquals(0, scan.basePackageClasses().length);

        var scanner = new ClassPathScanningCandidateComponentProvider(true);
        Set<String> candidates = scanner.findCandidateComponents(InIamApplication.class.getPackageName()).stream()
                .map(definition -> definition.getBeanClassName())
                .collect(Collectors.toSet());
        for (Class<?> component : Set.of(CatalogFacade.class, IamReadRequestConfiguration.class,
                IamMapperConfiguration.class, PlatformApplicationAPI.class)) {
            assertTrue(candidates.contains(component.getName()), component.getName());
        }
    }
}
