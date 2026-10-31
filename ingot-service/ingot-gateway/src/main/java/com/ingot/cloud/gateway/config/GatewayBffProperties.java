package com.ingot.cloud.gateway.config;

import java.util.ArrayList;
import java.util.List;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.utils.BffOriginPolicy;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Gateway 与 BFF 共用的同源应用注册及Cookie安全策略。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ingot.bff", ignoreUnknownFields = false)
public class GatewayBffProperties {

    /** 默认HTTPS；仅loopback DEV可关闭，必须与BFF一致。 */
    private boolean requireHttps = true;

    /** 两个应用各自唯一origin，兼容同IP不同端口。 */
    private List<BffAppRegistration> apps = new ArrayList<>();

    /** 启动即拒绝未知/重复origin及旧四站结构。 */
    @PostConstruct
    public void validateProductionRegistry() {
        BffOriginPolicy.validate(apps, requireHttps);
    }

}
