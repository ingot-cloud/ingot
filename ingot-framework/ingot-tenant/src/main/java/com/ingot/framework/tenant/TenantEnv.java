package com.ingot.framework.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : TenantEnv.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/6/23.</p>
 * <p>Time         : 10:10 上午.</p>
 */
@Slf4j
public class TenantEnv {
    @FunctionalInterface
    public interface Run {
        void exec() throws Exception;
    }

    @FunctionalInterface
    public interface Apply<R> {
        R exec() throws Exception;
    }

    /**
     * 切换租户环境运行
     * @param tenantId 目标租户
     * @param func 执行操作
     */
    public static void runAs(Integer tenantId, Run func) {
        final Integer pre = TenantContextHolder.get();
        try {
            log.trace("TenantEnv - 切换租户环境 {} -> {}, 开始执行", pre, tenantId);
            TenantContextHolder.set(tenantId);
            func.exec();
        }
        catch (Exception e) {
            throw new TenantEnvException(e.getMessage(), e);
        }
        finally {
            log.trace("TenantEnv - 还原租户 {} <- {}, 执行结束", pre, tenantId);
            TenantContextHolder.set(pre);
        }
    }

    /**
     * 切换租户环境运行
     * @param tenantId 目标租户
     * @param func 执行操作
     * @return 返回结果
     */
    public static <T> T applyAs(Integer tenantId, Apply<T> func) {
        final Integer pre = TenantContextHolder.get();
        try {
            log.trace("TenantEnv - 切换租户环境 {} -> {}, 开始执行", pre, tenantId);
            TenantContextHolder.set(tenantId);
            return func.exec();
        }
        catch (Exception e) {
            throw new TenantEnvException(e.getMessage(), e);
        }
        finally {
            log.trace("TenantEnv - 还原租户 {} <- {}, 执行结束", pre, tenantId);
            TenantContextHolder.set(pre);
        }
    }

    public static class TenantEnvException extends RuntimeException {

        public TenantEnvException(String message, Throwable cause) {
            super(message, cause);
        }

        public TenantEnvException(Throwable cause) {
            super(cause);
        }

    }
}
