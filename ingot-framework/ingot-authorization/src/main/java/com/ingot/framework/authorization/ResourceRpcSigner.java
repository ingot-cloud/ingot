package com.ingot.framework.authorization;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.SignedResourceObjectRequest;

/**
 * <p>资源候选服务的调用方证明，签名覆盖原始身份、资源、分页和范围；密钥仅来自服务器配置。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ResourceRpcSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private static final int MIN_SECRET_BYTES = 32;

    private static final long MAX_CLOCK_SKEW_SECONDS = 30;

    private ResourceRpcSigner() {
    }

    /**
     * 创建签名请求。
     * @param payload 原始JSON
     * @param secret 专用密钥
     * @return 签名封装
     */
    public static SignedResourceObjectRequest sign(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        return new SignedResourceObjectRequest(payload, timestamp, signature(timestamp, payload, secret));
    }

    /**
     * 校验可信调用方和时间窗口。
     * @param request 签名内容
     * @param secret 接收服务配置
     */
    public static void verify(SignedResourceObjectRequest request, String secret) {
        if (request == null || request.payload() == null || request.signature() == null
                || (request.timestamp() < Instant.now().getEpochSecond() - MAX_CLOCK_SKEW_SECONDS
                        || request.timestamp() > Instant.now().getEpochSecond() + MAX_CLOCK_SKEW_SECONDS)) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
        String expected = signature(request.timestamp(), request.payload(), secret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                request.signature().getBytes(StandardCharsets.US_ASCII))) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
    }

    private static String signature(long timestamp, String payload, String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("资源RPC密钥至少需要32字节");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal((timestamp + "\n" + payload).getBytes(StandardCharsets.UTF_8)));
        }
        catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("资源RPC签名不可用", exception);
        }
    }

}
