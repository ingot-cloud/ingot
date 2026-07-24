package com.ingot.framework.security.credential;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy.Generation;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.model.InitialPasswordConfig;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.policy.PasswordStrengthPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import com.ingot.framework.security.credential.service.impl.RemoteCredentialPolicyLoader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RemoteCredentialPolicyLoader} 初始密码取值与策略编译单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class RemoteCredentialPolicyLoaderTest {

    private final PasswordEncoder encoder = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return String.valueOf(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return String.valueOf(rawPassword).equals(encodedPassword);
        }
    };

    private RemoteCredentialPolicyLoader loader(List<CredentialPolicyConfigVO> configs) {
        CredentialPolicyConfigService configService = () -> configs;
        return new RemoteCredentialPolicyLoader(configService, encoder);
    }

    private static CredentialPolicyConfigVO vo(CredentialPolicyType type, Map<String, Object> config, int priority) {
        CredentialPolicyConfigVO v = new CredentialPolicyConfigVO();
        v.setPolicyType(type.getValue());
        v.setPolicyConfig(config);
        v.setPriority(priority);
        v.setEnabled(Boolean.TRUE);
        return v;
    }

    @Test
    void getInitialPasswordConfig_hit_mapsFromRemote() {
        Map<String, Object> map = Map.of(
                "generation", "FIXED",
                "length", 8,
                "fixedPassword", "P@ss2026",
                "validHours", 24,
                "oneTime", false,
                "forceChangeOnFirstLogin", false);
        RemoteCredentialPolicyLoader loader = loader(List.of(vo(CredentialPolicyType.INITIAL_PASSWORD, map, 40)));

        InitialPasswordConfig config = loader.getInitialPasswordConfig();

        assertEquals(Generation.FIXED, config.generation());
        assertEquals(8, config.length());
        assertEquals("P@ss2026", config.fixedPassword());
        assertEquals(24, config.validHours());
        assertEquals(false, config.oneTime());
        assertEquals(false, config.forceChangeOnFirstLogin());
    }

    @Test
    void getInitialPasswordConfig_missing_returnsDefaults() {
        RemoteCredentialPolicyLoader loader = loader(List.of());

        InitialPasswordConfig config = loader.getInitialPasswordConfig();

        assertEquals(InitialPasswordConfig.defaults(), config);
    }

    @Test
    void loadPolicies_excludesInitialPassword() {
        Map<String, Object> strength = Map.of("minLength", 8);
        Map<String, Object> initial = Map.of("generation", "RANDOM");
        RemoteCredentialPolicyLoader loader = loader(List.of(
                vo(CredentialPolicyType.STRENGTH, strength, 10),
                vo(CredentialPolicyType.INITIAL_PASSWORD, initial, 40)));

        List<PasswordPolicy> policies = loader.loadPolicies();

        assertEquals(1, policies.size(), "初始密码不参与校验策略列表");
        assertTrue(policies.get(0) instanceof PasswordStrengthPolicy);
    }

    @Test
    void loadPolicies_reflectsChainChangesAcrossCalls_noPermanentShortCircuit() {
        // 底层 getAll() 的返回随降级/恢复而变化：加载器不应有永久编译缓存，每次都应反映当前链路结果
        AtomicReference<List<CredentialPolicyConfigVO>> current = new AtomicReference<>(
                List.of(vo(CredentialPolicyType.STRENGTH, Map.of("minLength", 8), 10)));
        CredentialPolicyConfigService configService = current::get;
        RemoteCredentialPolicyLoader loader = new RemoteCredentialPolicyLoader(configService, encoder);

        assertEquals(1, loader.loadPolicies().size(), "首次应编译出 1 条强度策略");

        // 模拟远程无策略（合法空 / 降级到空快照）
        current.set(List.of());
        assertTrue(loader.loadPolicies().isEmpty(), "链路变为空后再次加载应反映为空，而非返回旧编译结果");

        // 模拟恢复：重新出现策略
        current.set(List.of(vo(CredentialPolicyType.STRENGTH, Map.of("minLength", 12), 10)));
        assertEquals(1, loader.loadPolicies().size(), "链路恢复后再次加载应反映最新结果");
    }
}
