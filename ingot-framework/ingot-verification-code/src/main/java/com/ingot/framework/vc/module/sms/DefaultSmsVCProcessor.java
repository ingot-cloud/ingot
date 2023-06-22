package com.ingot.framework.vc.module.sms;

import com.ingot.framework.core.utils.reactive.WebUtils;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.module.reactive.AbstractVCProcessor;
import com.ingot.framework.vc.module.reactive.ReactorUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : DefaultSmsVCProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 3:18 PM.</p>
 */
public class DefaultSmsVCProcessor extends AbstractVCProcessor {
    private final SmsCodeSender smsCodeSender;

    public DefaultSmsVCProcessor(VCRepository repository, SmsCodeSender smsCodeSender) {
        super(repository);
        this.smsCodeSender = smsCodeSender;
    }

    @Override
    protected Mono<ServerResponse> send(ServerRequest request, VC validateCode) {
        String receiver = ReactorUtils.getReceiver(request);
        String remoteIP = WebUtils.getRemoteIP(request);
        // 发送短息
        smsCodeSender.send(receiver, remoteIP, validateCode);
        return ReactorUtils.defaultSendSuccess();
    }
}
