package com.ingot.framework.security.provider.error;

import com.ingot.framework.security.exception.oauth2.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.DefaultThrowableAnalyzer;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.*;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.web.util.ThrowableAnalyzer;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/**
 * <p>Description  : OAuth2 异常解释器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:24.</p>
 */
@Slf4j
public class IngotWebResponseExceptionTranslator implements WebResponseExceptionTranslator<OAuth2Exception> {

    private final ThrowableAnalyzer throwableAnalyzer = new DefaultThrowableAnalyzer();

    @Override
    public ResponseEntity<OAuth2Exception> translate(Exception e) throws Exception {
        // Try to extract a SpringSecurityException from the stacktrace
        Throwable[] causeChain = throwableAnalyzer.determineCauseChain(e);

        Exception ase = (AuthenticationException) throwableAnalyzer
                .getFirstThrowableOfType(AuthenticationException.class, causeChain);
        if (ase != null) {
            return handleOAuth2Exception(new UnauthorizedException(e.getMessage(), e));
        }

        ase = handleBadRequestException(causeChain);
        if (ase != null) {
            return handleOAuth2Exception(new BadRequestException(e.getMessage(), e));
        }

        ase = (AccessDeniedException) throwableAnalyzer
                .getFirstThrowableOfType(AccessDeniedException.class, causeChain);
        if (ase != null) {
            return handleOAuth2Exception(new ForbiddenException(ase.getMessage(), ase));
        }

        ase = (HttpRequestMethodNotSupportedException) throwableAnalyzer
                .getFirstThrowableOfType(HttpRequestMethodNotSupportedException.class, causeChain);
        if (ase != null) {
            return handleOAuth2Exception(new MethodNotAllowedException(ase.getMessage(), ase));
        }

        ase = (OAuth2Exception) throwableAnalyzer
                .getFirstThrowableOfType(OAuth2Exception.class, causeChain);

        if (ase != null) {
            return handleOAuth2Exception((OAuth2Exception) ase);
        }

        return handleOAuth2Exception(new ServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), e));

    }

    private ResponseEntity<OAuth2Exception> handleOAuth2Exception(OAuth2Exception e) {

        int status = e.getHttpErrorCode();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "no-store");
        headers.set(HttpHeaders.PRAGMA, "no-cache");
        if (status == HttpStatus.UNAUTHORIZED.value() || (e instanceof InsufficientScopeException)) {
            headers.set(HttpHeaders.WWW_AUTHENTICATE, String.format("%s %s", OAuth2AccessToken.BEARER_TYPE, e.getSummary()));
        }

        if (e instanceof IngotOAuth2Exception) {
            return new ResponseEntity<>(e, headers, HttpStatus.valueOf(status));
        }

        return new ResponseEntity<>(new IngotOAuth2Exception(e), headers, HttpStatus.valueOf(status));
    }

    private Exception handleBadRequestException(Throwable[] causeChain) {

        Exception ase = (InvalidScopeException) throwableAnalyzer
                .getFirstThrowableOfType(InvalidScopeException.class, causeChain);
        if (ase != null) {
            return ase;
        }

        ase = (UnsupportedGrantTypeException) throwableAnalyzer
                .getFirstThrowableOfType(UnsupportedGrantTypeException.class, causeChain);
        return ase;
    }
}
