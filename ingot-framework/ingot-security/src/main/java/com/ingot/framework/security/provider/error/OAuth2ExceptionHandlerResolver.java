package com.ingot.framework.security.provider.error;

import com.ingot.framework.security.exception.oauth2.IngotOAuth2Exception;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>Description  : OAuth2ExceptionHandlerResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/30.</p>
 * <p>Time         : 2:15 下午.</p>
 */
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice
public class OAuth2ExceptionHandlerResolver {
    private final IngotWebResponseExceptionTranslator translator = new IngotWebResponseExceptionTranslator();

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<OAuth2Exception> handleException(OAuth2Exception e) throws Exception {
        return translator.translate(e);
    }

    @ExceptionHandler(IngotOAuth2Exception.class)
    public ResponseEntity<OAuth2Exception> handleException(IngotOAuth2Exception e) throws Exception {
        return translator.translate(e);
    }
}
