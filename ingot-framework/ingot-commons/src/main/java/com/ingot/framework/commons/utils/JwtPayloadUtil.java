package com.ingot.framework.commons.utils;

import java.util.Base64;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>不验签地读取 JWT payload 声明，供边缘组件提取会话定位信息。</p>
 *
 * <p><strong>仅用于路由、限流、会话定位等非鉴权用途。</strong>本工具不校验签名与有效期，
 * Token 真伪必须由资源服务器判定；解析失败一律返回 {@code null}，调用方自行决定降级行为。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.commons.constants.InJwtClaimNames
 */
@Slf4j
@UtilityClass
public class JwtPayloadUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final char SEGMENT_DELIMITER = '.';

    /**
     * 读取指定声明并转为字符串。
     *
     * @param jwt       完整 JWT 字符串（不含 {@code Bearer } 前缀）
     * @param claimName 声明名
     * @return 声明值；JWT 格式非法、解码失败或声明缺失时返回 {@code null}
     */
    public static String readClaim(String jwt, String claimName) {
        if (StrUtil.isEmpty(jwt)) {
            return null;
        }
        int firstDot = jwt.indexOf(SEGMENT_DELIMITER);
        if (firstDot < 0) {
            return null;
        }
        int secondDot = jwt.indexOf(SEGMENT_DELIMITER, firstDot + 1);
        if (secondDot < 0) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(jwt.substring(firstDot + 1, secondDot));
            Map<String, Object> claims = MAPPER.readValue(decoded, new TypeReference<>() {
            });
            Object raw = claims.get(claimName);
            if (raw == null) {
                return null;
            }
            // 数值型声明（如用户 ID）统一按整数输出，避免 Jackson 推断出 9.0 这类形态
            if (raw instanceof Number number) {
                return String.valueOf(number.longValue());
            }
            return StrUtil.emptyToNull(String.valueOf(raw).trim());
        } catch (Exception e) {
            log.debug("[JwtPayloadUtil] 解析 JWT payload 失败: claim={}", claimName, e);
            return null;
        }
    }
}
