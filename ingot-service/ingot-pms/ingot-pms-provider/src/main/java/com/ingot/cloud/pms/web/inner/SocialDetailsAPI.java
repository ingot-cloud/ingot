package com.ingot.cloud.pms.web.inner;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.biz.SocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.config.annotation.web.configuration.PermitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : SocialDetailsAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 16:03.</p>
 */
@Slf4j
@Permit(mode = PermitMode.INNER)
@RestController
@RequestMapping(value = "/inner/social")
@RequiredArgsConstructor
public class SocialDetailsAPI implements RShortcuts {
    private final SocialDetailsService socialDetailsService;

    @GetMapping("/detailList/{type}")
    public R<List<SysSocialDetails>> getSocialDetailsByType(@PathVariable String type) {
        return ok(socialDetailsService.getSocialDetailsByType(SocialTypeEnum.get(type)));
    }
}
