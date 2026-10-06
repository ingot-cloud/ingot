package com.ingot.framework.commons.model.iam.extension;

import java.util.*;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.*;

/**
 * <p>对象查询签名封装，不接受客户端自行指定服务地址。</p>
 *
 * @param payload 原始JSON签名内容
 * @param timestamp UTC纪元秒
 * @param signature HMAC-SHA256签名
 * @author jy
 * @since 1.0.0
 */
public record SignedResourceObjectRequest(String payload, long timestamp, String signature) {

}
