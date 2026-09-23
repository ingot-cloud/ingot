package com.ingot.cloud.iam.support;

import java.util.List;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.oss.jackson.OssUrlSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IamOssJacksonConfigurationTest {

    @Test
    void deserializeStripsPresignedUrl() throws Exception {
        ObjectMapper mapper = mapper(null);
        MemberCreateInput input = mapper.readValue("""
                {"accountId":"1","avatar":"https://minio.local/ingot/user/avatar/a.png?X-Amz-Expires=3600","departments":[]}
                """, MemberCreateInput.class);
        assertEquals("ingot/user/avatar/a.png", input.avatar());
    }

    @Test
    void serializeSignsStoredPath() throws Exception {
        OssService oss = mock(OssService.class);
        when(oss.getObjectURL("ingot/user/avatar/a.png")).thenReturn("https://signed.example/ingot/user/avatar/a.png");
        ObjectMapper mapper = mapper(oss);
        MemberRecord record = new MemberRecord("1", "成员", "ingot/user/avatar/a.png", null, null, "alice",
                MemberStatus.ACTIVE, List.of());
        String json = mapper.writeValueAsString(record);
        assertTrue(json.contains("https://signed.example/ingot/user/avatar/a.png"));
    }

    private static ObjectMapper mapper(OssService oss) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new IamOssJacksonConfiguration().iamOssAvatarMixins().customize(builder);
        if (oss != null) {
            builder.handlerInstantiator(new OssHandlerInstantiator(oss));
        }
        return builder.build();
    }

    private static final class OssHandlerInstantiator extends HandlerInstantiator {
        private final OssService oss;

        private OssHandlerInstantiator(OssService oss) {
            this.oss = oss;
        }

        @Override
        public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated,
                                                        Class<?> deserClass) {
            return null;
        }

        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated,
                                                       Class<?> keyDeserClass) {
            return null;
        }

        @Override
        public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated,
                                                    Class<?> serClass) {
            if (serClass == OssUrlSerializer.class) {
                return new OssUrlSerializer(oss);
            }
            return null;
        }

        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated,
                                                                  Class<?> builderClass) {
            return null;
        }

        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated,
                                                     Class<?> resolverClass) {
            return null;
        }
    }
}
