package com.ingot.cloud.pms.core;

import com.ingot.framework.id.BizGenerator;
import com.ingot.framework.id.config.IdAutoConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * <p>Description  : BizIdGen.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/20.</p>
 * <p>Time         : 5:02 PM.</p>
 */
@Component
@AutoConfigureAfter(IdAutoConfig.class)
@RequiredArgsConstructor
public class BizIdGen {
    private static final Random RANDOM = new Random();
    private final BizGenerator bizGenerator;

    /**
     * 组织编码
     *
     * @return 组织编码
     */
    public String genOrgCode() {
        long id = bizGenerator.getId("org_role_code");
        return String.format("org_role_%d", (id << 6 | RANDOM.nextInt(64)));
    }

    /**
     * 组织角色编码
     *
     * @return 角色编码
     */
    public String genOrgRoleCode() {
        long id = bizGenerator.getId("org_role_code");
        return String.format("org_role_%d", (id << 6 | RANDOM.nextInt(64)));
    }

    /**
     * 生成APP ID
     *
     * @return APP ID
     */
    public String genAppIdCode() {
        long id = bizGenerator.getId("org_role_code");
        return String.format("in%d", (id << 6 | RANDOM.nextInt(64)));
    }
}
