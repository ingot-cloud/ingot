package com.ingot.framework.security.crypto.web;

import java.util.List;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.crypto.hybrid.HybridKeyManager;
import com.ingot.framework.security.crypto.hybrid.PublicKeyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>信封加密公钥下发端点，向前端下发可用公钥及版本（kid）。</p>
 *
 * <p>公钥用于每请求临时 CEK 的 RSA-OAEP 包裹，私钥不下发。是否启用由
 * {@code ingot.crypto.hybrid.public-key-endpoint-enabled} 控制。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 端点需在网关加入匿名放行白名单方可被前端初始化调用。
 */
@RestController
@RequiredArgsConstructor
public class HybridPublicKeyController {
    private final HybridKeyManager hybridKeyManager;

    /**
     * 获取公钥列表。
     *
     * @return 公钥信息列表（含活跃标记）
     */
    @GetMapping("/crypto/public-keys")
    public R<List<PublicKeyInfo>> publicKeys() {
        return R.ok(hybridKeyManager.publicKeyInfos());
    }
}
