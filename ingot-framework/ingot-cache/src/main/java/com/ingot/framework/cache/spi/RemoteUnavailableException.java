package com.ingot.framework.cache.spi;

/**
 * <p>标识底层数据源不可用的信号异常，是触发 {@code remote → LKG → 地板} 降级阶梯的唯一入口。</p>
 *
 * <p>只有真正的不可用（连接失败、超时、远端返回业务失败码）才应抛出本异常。远端调用成功但数据为空
 * 属于<b>合法空</b>，必须正常返回而非抛出，否则会把「当前确实没有配置」误判为故障并污染降级链。</p>
 *
 * <p>各消费模块可继承本类以保留自己的异常类型与错误信息，例如网关侧的
 * {@code PolicyRemoteUnavailableException}。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see CacheValueLoader
 */
public class RemoteUnavailableException extends RuntimeException {

    public RemoteUnavailableException(String message) {
        super(message);
    }

    public RemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
