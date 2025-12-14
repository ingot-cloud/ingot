package com.ingot.cloud.pms.web.inner;

import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : InnerIdAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/14.</p>
 * <p>Time         : 15:24.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/id")
@RequiredArgsConstructor
public class InnerIdAPI implements RShortcuts {
    private final BizIdGen bizIdGen;

    @GetMapping("/appId")
    public R<String> appId() {
        return ok(bizIdGen.genAppIdCode());
    }
}
