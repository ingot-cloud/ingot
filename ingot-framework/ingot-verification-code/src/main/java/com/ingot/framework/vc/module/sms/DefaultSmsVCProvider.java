package com.ingot.framework.vc.module.sms;

import com.ingot.framework.core.utils.WebUtils;
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

    public DefaultSmsVCProvider(VCRepository repository, SmsCodeSender smsCodeSender) {
        super(repository);
        this.smsCodeSender = smsCodeSender;
    }

    @Override
    protected void send(ServletWebRequest request, VC validateCode) throws Exception {
        String receiver = ServletUtils.getReceiver(request);
        String remoteIP = WebUtils.getRemoteIP(request.getRequest());

//        ValidateCodeResultDto data = new ValidateCodeResultDto();
//        data.setResult(true);
//        IngotResponse<?> result = ResponseWrapper.ok(data);
//        // 统一处理短信流量
//        try {
//            Utils.checkSendSmsCount(receiver, remoteIP, redisTemplate, smsCodeProperties);
//            smsCodeSender.send(receiver, validateCode, remoteIP);
//        } catch (Exception e) {
//            log.error("Servlet SmsCodeProcessor 校验短信数量, e={}", e.getMessage(), e);
//            data.setResult(false);
//            result = ResponseWrapper.error500WithData(data, e.getMessage());
//        }
//        String json = objectMapper.writeValueAsString(result);
//        HttpServletResponse response = request.getResponse();
//        response.setContentType(MediaType.APPLICATION_JSON_UTF8.toString());
//        response.getWriter().write(json);
    }
}
