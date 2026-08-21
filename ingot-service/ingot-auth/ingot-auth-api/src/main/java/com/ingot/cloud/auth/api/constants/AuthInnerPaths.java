package com.ingot.cloud.auth.api.constants;

/**
 * <p>Auth 服务 Inner 接口路径常量，Feign 契约与 Controller 映射共用同一份定义。</p>
 *
 * <p>Feign 侧使用「前缀 + 子路径」的编译期拼接，Controller 侧用前缀做类级
 * {@code @RequestMapping}、子路径做方法级映射，两侧不再各写一遍字面量。</p>
 *
 * <pre>{@code
 * @RequestMapping(AuthInnerPaths.SESSION)
 * class InnerSessionAPI {
 *     @GetMapping(AuthInnerPaths.Session.PAGE)
 *     R<InnerSessionPageVO> page(InnerSessionQueryDTO params) { ... }
 * }
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AuthInnerPaths {

    /**
     * Inner 接口统一前缀，网关会剥离外部请求携带的内部标识头，故该前缀仅内网可达。
     */
    String INNER = "/inner";

    /**
     * 会话执行面前缀。
     */
    String SESSION = INNER + "/session";

    /**
     * <p>会话执行面子路径与路径变量名。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    interface Session {

        /**
         * 分页查询在线会话。
         */
        String PAGE = "/page";

        /**
         * 单会话详情或撤销，路径变量 {@link #VARIABLE_SID}。
         */
        String SID = "/{sid}";

        /**
         * 按用户查询或撤销会话。
         */
        String USER = "/user";

        /**
         * 按租户批量撤销会话，路径变量 {@link #VARIABLE_TENANT_ID}。
         */
        String TENANT = "/tenant/{tenantId}";

        String VARIABLE_SID = "sid";

        String VARIABLE_TENANT_ID = "tenantId";
    }
}
