package com.ingot.framework.crypto.jackson;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.ingot.framework.crypto.annotation.InFieldDecrypt;

/**
 * <p>Description  : CryptoAnnotationIntrospector.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 09:09.</p>
 */
public class CryptoAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public Object findDeserializer(Annotated a) {
        Object deser = super.findDeserializer(a);
        if (deser != null) {
            return deser;
        }

        InFieldDecrypt ann = _findAnnotation(a, InFieldDecrypt.class);
        if (ann != null) {
            return CryptoDeserializer.class;
        }
        return null;
    }
}
