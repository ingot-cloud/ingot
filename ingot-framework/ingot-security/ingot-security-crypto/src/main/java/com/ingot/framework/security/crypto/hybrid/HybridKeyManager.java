package com.ingot.framework.security.crypto.hybrid;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.*;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.refresh.NacosConfigRefreshEvent;
import com.ingot.framework.commons.constants.NacosConstants;
import com.ingot.framework.commons.utils.crypto.RSAUtil;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.utils.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

/**
 * <p>信封加密密钥管理器，管理多版本（kid）RSA 密钥对。</p>
 *
 * <p>加载配置中的多版本密钥对，向前端下发活跃/历史公钥，并用私钥以 RSA-OAEP-256 解包内容密钥 CEK；
 * 私钥仅驻留服务端。密钥集以不可变快照持有，配置刷新时重新加载并原子替换，实现不停机轮换。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 监听 {@link NacosConfigRefreshEvent}，且仅当 dataId 为 {@link NacosConstants#IN_SECURITY_CRYPTO}
 * 时自动重建密钥快照；也可显式调用 {@link #refresh()}。
 * @implNote RSA-OAEP 使用 SHA-256 且 MGF1 亦为 SHA-256，以与 WebCrypto {@code RSA-OAEP(SHA-256)} 互通。
 */
@Slf4j
public class HybridKeyManager implements ApplicationListener<NacosConfigRefreshEvent> {
    /**
     * RSA-OAEP with SHA-256 (MGF1 SHA-256)，与 WebCrypto RSA-OAEP(SHA-256) 互通
     */
    private static final String RSA_OAEP_TRANSFORM = "RSA/ECB/OAEPPadding";

    private final InCryptoProperties properties;
    private volatile KeyStore keyStore;

    public HybridKeyManager(InCryptoProperties properties) {
        this.properties = properties;
        this.keyStore = load();
    }

    /**
     * 重新加载密钥集并原子替换当前快照，用于配置刷新后的密钥轮换。
     */
    public void refresh() {
        KeyStore reloaded = load();
        this.keyStore = reloaded;
        log.info(CryptoUtils.logMsg("密钥集已刷新 - activeKid={}, kids={}"),
                reloaded.activeKid(), reloaded.publicKeys().keySet());
    }

    @Override
    public void onApplicationEvent(NacosConfigRefreshEvent event) {
        if (!StrUtil.equals(event.getDataId(), NacosConstants.IN_SECURITY_CRYPTO)) {
            return;
        }
        refresh();
    }

    /**
     * 返回可下发的公钥列表（含活跃标记）。
     */
    public List<PublicKeyInfo> publicKeyInfos() {
        KeyStore snapshot = this.keyStore;
        List<PublicKeyInfo> result = new ArrayList<>(snapshot.publicKeys().size());
        snapshot.publicKeys().forEach((kid, key) -> result.add(new PublicKeyInfo(
                kid,
                HybridCryptoService.DEFAULT_ALG,
                Base64.encode(key.getEncoded()),
                kid.equals(snapshot.activeKid()))));
        return result;
    }

    /**
     * 用指定版本私钥解包内容密钥 CEK。
     *
     * @param kid          密钥版本
     * @param wrappedBase64 Base64(RSA-OAEP(CEK))
     * @return CEK 原始字节
     */
    public byte[] unwrapCek(String kid, String wrappedBase64) {
        PrivateKey privateKey = this.keyStore.privateKeys().get(kid);
        if (privateKey == null) {
            log.warn(CryptoUtils.logMsg("未知密钥版本 - kid={}"), kid);
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_KID_UNKNOWN);
        }
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORM);
            OAEPParameterSpec oaep = new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaep);
            return cipher.doFinal(Base64.decode(wrappedBase64));
        } catch (Exception e) {
            log.error(CryptoUtils.logMsg("CEK 解包失败 - kid={}"), kid, e);
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_KEY_UNWRAP_ERROR);
            return null;
        }
    }

    /**
     * 从当前配置加载密钥集，构建不可变快照。
     */
    private KeyStore load() {
        InCryptoProperties.Hybrid hybrid = properties.getHybrid();
        Map<String, PublicKey> publicKeys = new LinkedHashMap<>();
        Map<String, PrivateKey> privateKeys = new LinkedHashMap<>();
        Map<String, InCryptoProperties.KeyPair> keyPairs = hybrid.getKeyPairs();
        if (keyPairs != null) {
            keyPairs.forEach((kid, kp) -> {
                try {
                    if (StrUtil.isNotBlank(kp.getPublicKey())) {
                        publicKeys.put(kid, RSAUtil.getPublicKey(Base64.decode(kp.getPublicKey())));
                    }
                    if (StrUtil.isNotBlank(kp.getPrivateKey())) {
                        privateKeys.put(kid, RSAUtil.getPrivateKey(Base64.decode(kp.getPrivateKey())));
                    }
                } catch (Exception e) {
                    log.error(CryptoUtils.logMsg("加载密钥对失败 - kid={}"), kid, e);
                }
            });
        }
        return new KeyStore(hybrid.getActiveKid(),
                Collections.unmodifiableMap(publicKeys),
                Collections.unmodifiableMap(privateKeys));
    }

    /**
     * <p>密钥集的不可变快照。</p>
     *
     * @param activeKid   当前活跃密钥版本
     * @param publicKeys  各版本公钥
     * @param privateKeys 各版本私钥
     * @author jy
     * @since 1.0.0
     */
    private record KeyStore(String activeKid,
                            Map<String, PublicKey> publicKeys,
                            Map<String, PrivateKey> privateKeys) {
    }
}
