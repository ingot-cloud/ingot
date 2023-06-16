package com.ingot.framework.vc.module.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.module.servlet.AbstractVCProvider;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : DefaultEmailVCProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 4:46 PM.</p>
 */
public class DefaultEmailVCProvider extends AbstractVCProvider {
    private final EmailCodeSender emailCodeSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultEmailVCProvider(VCRepository repository, EmailCodeSender emailCodeSender) {
        super(repository);
        this.emailCodeSender = emailCodeSender;
    }

    @Override
    protected void send(ServletWebRequest request, VC validateCode) throws Exception {

    }
}
