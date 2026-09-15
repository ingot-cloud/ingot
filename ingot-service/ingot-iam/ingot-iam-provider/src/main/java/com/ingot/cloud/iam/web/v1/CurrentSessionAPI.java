package com.ingot.cloud.iam.web.v1;

import com.ingot.cloud.iam.session.SessionService;
import com.ingot.framework.commons.model.iam.Bootstrap;
import com.ingot.framework.commons.model.iam.CurrentCapabilities;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>返回当前认证身份的一致授权视图与精确操作集合。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 当前身份")
@RequestMapping("/v1/me")
@RequiredArgsConstructor
public class CurrentSessionAPI implements RShortcuts {
    private final SessionService sessions;

    /**
     * 返回当前身份一致授权视图。
     *
     * @return bootstrap
     */
    @Operation(summary = "当前身份一致授权视图")
    @GetMapping("/bootstrap")
    public R<Bootstrap> bootstrap() {
        return ok(sessions.bootstrap());
    }

    /**
     * 返回当前身份操作能力。
     *
     * @return 精确操作集合
     */
    @Operation(summary = "当前身份操作能力")
    @GetMapping("/capabilities")
    public R<CurrentCapabilities> capabilities() {
        return ok(sessions.capabilities());
    }
}
