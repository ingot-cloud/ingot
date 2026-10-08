package com.ingot.cloud.iam.support;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.api.model.dto.auth.LoginRecordDTO;
import com.ingot.cloud.iam.web.inner.InnerLoginRecordAPI;
import com.ingot.framework.commons.constants.TimeConstants;
import com.ingot.framework.commons.jackson.InJackson2ObjectMapperBuilderCustomizer;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.core.config.JacksonConfig;
import com.ingot.framework.security.account.domain.port.inbound.RecordLoginUseCase;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>验证业务 JSON 扩展与框架基础时间配置共存时，登录记录仍能通过 HTTP 正常解析。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class LoginRecordJacksonConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonConfig.class, JacksonAutoConfiguration.class));

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void iamCallbacksAcceptFrameworkTimeFormatWithOssExtension(boolean success) {
        runner.withUserConfiguration(IamOssJacksonConfiguration.class).run(context -> {
            assertNull(context.getStartupFailure());
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            String json = callbackJson(success);
            LoginRecordDTO dto = mapper.readValue(json, LoginRecordDTO.class);
            assertEquals(LocalDateTime.of(2026, 10, 3, 20, 38, 24), dto.getLoginAt());
            assertEquals("2026-10-03T20:38:24Z", mapper.readTree(mapper.writeValueAsString(dto))
                    .get("loginAt").asText());
            assertEquals("1001700", mapper.readTree(mapper.writeValueAsString(dto)).get("userId").asText());

            RecordLoginUseCase records = mock(RecordLoginUseCase.class);
            UserAccountPort accounts = mock(UserAccountPort.class);
            MockMvcBuilders.standaloneSetup(new InnerLoginRecordAPI(records, accounts))
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                    .build()
                    .perform(post("/inner/user/login/record").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isOk());
            if (success) {
                verify(records).recordSuccess(argThat(command -> command.getUserId().equals(dto.getUserId())));
            } else {
                verify(records).recordFailure(argThat(command -> command.getUserId().equals(dto.getUserId())));
            }
            verifyNoInteractions(accounts);
        });
    }

    @Test
    void memberCallbackRetainsTheSameTimeContract() {
        runner.withUserConfiguration(IamOssJacksonConfiguration.class).run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO dto = mapper.readValue(callbackJson(true),
                    com.ingot.cloud.member.api.model.dto.auth.LoginRecordDTO.class);
            assertEquals(LocalDateTime.of(2026, 10, 3, 20, 38, 24), dto.getLoginAt());
            assertEquals("2026-10-03T20:38:24Z", mapper.readTree(mapper.writeValueAsString(dto))
                    .get("loginAt").asText());
        });
    }

    @Test
    void ossAndLongIdContractsRemainActiveAlongsideTimeModule() {
        OssService oss = mock(OssService.class);
        when(oss.getObjectURL("ingot/user/avatar/a.png")).thenReturn("https://signed.example/avatar.png");
        runner.withUserConfiguration(IamOssJacksonConfiguration.class).withBean(OssService.class, () -> oss)
                .run(context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    MemberCreateInput input = mapper.readValue("""
                            {"accountId":"1","avatar":"https://minio.local/ingot/user/avatar/a.png?X-Amz-Expires=3600","departments":[]}
                            """, MemberCreateInput.class);
                    assertEquals("ingot/user/avatar/a.png", input.avatar());
                    MemberRecord record = new MemberRecord("1", "成员", "ingot/user/avatar/a.png", null, null, "alice",
                            MemberStatus.ACTIVE, List.of());
                    assertEquals("https://signed.example/avatar.png", mapper.readTree(mapper.writeValueAsString(record))
                            .get("avatar").asText());
                    assertEquals("\"9007199254740993\"", mapper.writeValueAsString(9007199254740993L));
                });
    }

    @Test
    void frameworkUtcContractAppliesWithoutBusinessExtensions() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertEquals(TimeConstants.UTC_ZONE_ID, mapper.getSerializationConfig().getTimeZone().getID());
            assertEquals("\"1970-01-01T00:00:00Z\"", mapper.writeValueAsString(new Date(0)));
            assertEquals(LocalDateTime.of(2026, 10, 3, 20, 38, 24),
                    mapper.readValue("\"2026-10-03T20:38:24Z\"", LocalDateTime.class));
        });
    }

    @Test
    void explicitMapperTimeZoneCannotChangeTimePointEncoding() {
        runner.withUserConfiguration(IamOssJacksonConfiguration.class, ExplicitTimeZoneConfiguration.class)
                .run(context -> {
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    assertEquals("America/New_York", mapper.getSerializationConfig().getTimeZone().getID());
                    assertEquals("\"1970-01-01T00:00:00Z\"", mapper.writeValueAsString(new Date(0)));
                });
    }

    private static String callbackJson(boolean success) {
        return """
                {"success":%s,"userId":"1001700","username":"tester","loginAt":"2026-10-03T20:38:24Z"}
                """.formatted(success);
    }

    /**
     * <p>模拟业务定制器指定 mapper 时区，验证时间点契约仍为 UTC。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ExplicitTimeZoneConfiguration {

        /**
         * mapper 时区定制不改变时间点的 UTC 输出。
         */
        @Bean
        InJackson2ObjectMapperBuilderCustomizer explicitTimeZone() {
            return builder -> builder.timeZone(TimeZone.getTimeZone("America/New_York"));
        }
    }
}
