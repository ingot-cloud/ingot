package com.ingot.framework.vc.module.email;

import com.ingot.framework.commons.utils.reactive.WebUtil;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.module.reactive.AbstractVCProcessor;
import com.ingot.framework.vc.module.reactive.ReactorUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : DefaultEmailVCProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/26.</p>
 * <p>Time         : 4:12 PM.</p>
 */
public class DefaultEmailVCProcessor extends AbstractVCProcessor {
    private final EmailCodeSender emailCodeSender;

    public DefaultEmailVCProcessor(VCRepository repository, EmailCodeSender emailCodeSender) {
        super(repository);
        this.emailCodeSender = emailCodeSender;
    }

    @Override
    protected Mono<ServerResponse> send(ServerRequest request, VC validateCode) {
        String receiver = ReactorUtils.getReceiver(request);
        String remoteIP = WebUtil.getRemoteIP(request);

        // 发送短息
        emailCodeSender.send(receiver, remoteIP, validateCode);
        return ReactorUtils.defaultSendSuccess();
    }
}
