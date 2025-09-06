package com.ingot.framework.vc.module.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.utils.WebUtils;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.module.servlet.AbstractVCProvider;
import com.ingot.framework.vc.module.servlet.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : SmsVCProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 5:10 PM.</p>
 */
@Slf4j
public class DefaultSmsVCProvider extends AbstractVCProvider {
    private final SmsCodeSender smsCodeSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultSmsVCProvider(VCRepository repository, SmsCodeSender smsCodeSender) {
        super(repository);
        this.smsCodeSender = smsCodeSender;
    }

    @Override
    protected void send(ServletWebRequest request, VC validateCode) throws Exception {
        String receiver = ServletUtils.getReceiver(request);
        String remoteIP = WebUtils.getRemoteIP(request.getRequest());

        // 发送短息
        smsCodeSender.send(receiver, remoteIP, validateCode);
        ServletUtils.defaultSendSuccess(request, objectMapper);
    }
}
