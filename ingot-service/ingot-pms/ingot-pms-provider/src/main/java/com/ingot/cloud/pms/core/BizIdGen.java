package com.ingot.cloud.pms.core;

import java.util.Random;

import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.id.BizGenerator;
import com.ingot.framework.id.config.IdAutoConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.stereotype.Component;

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
        return gen("org_code", "org_%d");
    }

    /**
     * 组织角色编码
     *
     * @return 角色编码
     */
    public String genOrgRoleCode() {
        return gen("org_role_code", RoleConstants.ORG_ROLE_CODE_PREFIX);
    }

    /**
     * 组织app角色编码
     *
     * @return 角色编码
     */
    public String genOrgAppRoleCode() {
        return gen("org_role_code", "org_app_role_");
    }

    /**
     * 生成APP ID
     *
     * @return APP ID
     */
    public String genAppIdCode() {
        return gen("app_id", "in_");
    }

    /**
     * 根据key和前缀生成业务ID
     *
     * @param key    业务Key
     * @param prefix 前缀
     * @return 业务ID
     */
    public String gen(String key, String prefix) {
        long id = bizGenerator.getId(key);
        return format(prefix, id);
    }

    private String format(String prefix, long id) {
        return String.format("%s%d", prefix, (id << 6 | RANDOM.nextInt(64)));
    }
}
