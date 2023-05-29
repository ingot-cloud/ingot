package com.ingot.framework.vc.module.sms;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.core.utils.WebUtils;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.module.servlet.AbstractVCProvider;
import com.ingot.framework.vc.module.servlet.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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

        // 响应结果
        HttpServletResponse response = request.getResponse();
        assert response != null;
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(R.ok(true)));
        response.flushBuffer();
    }
}
