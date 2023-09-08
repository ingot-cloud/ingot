package com.ingot.framework.security.jackson2;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * <p>Description  : IngotSecurityJackson2Modules.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/8.</p>
 * <p>Time         : 2:29 PM.</p>
 */
@Slf4j
public class IngotSecurityJackson2Modules {
    private static final String ingotOAuth2AuthorizationServerJackson2ModuleClass =
            "com.ingot.framework.security.oauth2.server.authorization.jackson2.IngotOAuth2AuthorizationServerJackson2Module";
    private static final boolean ingotOAuth2Present;

    static {
        ClassLoader classLoader = IngotSecurityJackson2Modules.class.getClassLoader();
        ingotOAuth2Present = ClassUtils.isPresent(ingotOAuth2AuthorizationServerJackson2ModuleClass, classLoader);
    }

    public static void registerModules(ObjectMapper objectMapper, ClassLoader classLoader) {
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        if (ingotOAuth2Present) {
            addToModulesList(classLoader, securityModules, ingotOAuth2AuthorizationServerJackson2ModuleClass);
        }
        objectMapper.registerModules(securityModules);
    }

    private static void addToModulesList(ClassLoader loader, List<Module> modules, String className) {
        Module module = loadAndGetInstance(className, loader);
        if (module != null) {
            modules.add(module);
        }
    }

    @SuppressWarnings("unchecked")
    private static Module loadAndGetInstance(String className, ClassLoader loader) {
        try {
            Class<? extends Module> securityModule = (Class<? extends Module>) ClassUtils.forName(className, loader);
            if (securityModule != null) {
                log.debug("Loaded module {}, now registering", className);
                return securityModule.getDeclaredConstructor().newInstance();
            }
        } catch (Exception ex) {
            log.debug("Cannot load module {}", className, ex);
        }
        return null;
    }
}
