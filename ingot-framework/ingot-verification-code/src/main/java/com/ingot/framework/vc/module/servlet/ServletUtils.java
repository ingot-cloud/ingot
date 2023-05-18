package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.common.IngotVCMessageSource;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 2:27 PM.</p>
 */
@Slf4j
public class ServletUtils {

    /**
     * 获取接收者
     *
     * @param request {@link ServletWebRequest}
     * @return 接收者信息
     */
    public static String getReceiver(ServletWebRequest request) {
        return getFromRequest(request, VCConstants.QUERY_PARAMS_RECEIVER);
    }

    /**
     * 获取验证码
     *
     * @param request {@link ServletWebRequest}
     * @return 验证码
     */
    public static String getCode(ServletWebRequest request) {
        return getFromRequest(request, VCConstants.QUERY_PARAMS_CODE);
    }

    private static String getFromRequest(ServletWebRequest request, String key) {
        try {
            return ServletRequestUtils.getStringParameter(request.getRequest(), key);
        } catch (Exception e) {
            log.error("[验证码] - getFromRequest 异常", e);
            throw new VCException(IngotVCMessageSource.getAccessor()
                    .getMessage("vc.common.illegalArgument",
                            new String[]{key}));
        }
    }
}
