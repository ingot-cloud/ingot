package com.ingot.framework.core.config;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.error.GlobalExceptionHandlerResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>独立业务服务无需 IAM 即可自动继承 JSON、query、form 的 UTC 时间契约。</p>
 * @author jy
 * @since 1.0.0
 */
class ApiTimeAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonConfig.class, JacksonAutoConfiguration.class));

    @Test
    void independentServiceUsesTheSameStrictContractForJsonQueryAndForm() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            DefaultFormattingConversionService conversion = new DefaultFormattingConversionService();
            new WebConfig().addFormatters(conversion);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new TimeAPI())
                    .setControllerAdvice(new GlobalExceptionHandlerResolver())
                    .setConversionService(conversion)
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
            String iso = "2026-10-08T09:00:00.123456+08:00";
            mvc.perform(get("/times").param("at", iso)).andExpect(status().isOk())
                    .andExpect(content().string("\"2026-10-08T01:00:00.123456Z\""));
            mvc.perform(post("/times/form").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("at", iso))
                    .andExpect(status().isOk()).andExpect(content().string("\"2026-10-08T01:00:00.123456Z\""));
            mvc.perform(post("/times").contentType(MediaType.APPLICATION_JSON).content("""
                    {"instant":"2026-10-08T09:00:00+08:00","legacy":"2026-10-08T01:00:00Z",
                    "date":"2026-10-08T01:00:00Z","day":"2026-10-08","time":"09:00:00","duration":"PT24H","id":"9007199254740993"}
                    """)).andExpect(status().isOk()).andExpect(jsonPath("$.instant").value("2026-10-08T01:00:00Z"))
                    .andExpect(jsonPath("$.legacy").value("2026-10-08T01:00:00Z"))
                    .andExpect(jsonPath("$.date").value("2026-10-08T01:00:00Z"))
                    .andExpect(jsonPath("$.day").value("2026-10-08")).andExpect(jsonPath("$.time").value("09:00:00"))
                    .andExpect(jsonPath("$.id").value("9007199254740993"));
            for (String invalid : new String[]{"2026-10-08 09:00:00", "2026-10-08T09:00:00", "", "2026-02-30T09:00:00Z"}) {
                mvc.perform(get("/times").param("at", invalid)).andExpect(status().isBadRequest());
                if (!invalid.isEmpty()) {
                    mvc.perform(get("/times/optional").param("at", invalid)).andExpect(status().isBadRequest());
                    mvc.perform(get("/times/list").param("at", invalid)).andExpect(status().isBadRequest());
                }
                mvc.perform(post("/times/form").param("at", invalid)).andExpect(status().isBadRequest());
                mvc.perform(post("/times").contentType(MediaType.APPLICATION_JSON).content("{\"instant\":\"" + invalid + "\"}"))
                        .andExpect(status().isBadRequest());
            }
            mvc.perform(post("/times").contentType(MediaType.APPLICATION_JSON).content("{\"instant\":123}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/times").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"date\":\"+999999999-01-01T00:00:00Z\"}"))
                    .andExpect(status().isBadRequest());
            assertEquals("2026-10-08T01:00:00Z", conversion.convert(LocalDateTime.of(2026, 10, 8, 1, 0), String.class));
        });
    }

    /** 与 IAM 无关的业务 DTO，各字段保持自身时间语义。 */
    record Payload(Instant instant, LocalDateTime legacy, Date date, LocalDate day, LocalTime time, Duration duration, Long id) { }

    /** 用于验证 form 绑定的业务请求。 */
    public static class TimeForm {
        /** UTC 时间点。 */
        private LocalDateTime at;
        /** 返回 UTC 时间点。 */
        public LocalDateTime getAt() { return at; }
        /** 设置表单解析后的 UTC 时间点。 */
        public void setAt(LocalDateTime value) { at = value; }
    }

    /** 独立业务 API，仅依赖公共框架。 */
    @RestController
    static class TimeAPI {
        @GetMapping("/times")
        public LocalDateTime query(@RequestParam("at") LocalDateTime at) { return at; }
        @GetMapping("/times/optional")
        public Instant optional(@RequestParam("at") Optional<Instant> at) { return at.orElse(null); }
        @GetMapping("/times/list")
        public List<Instant> list(@RequestParam("at") List<Instant> at) { return at; }
        @PostMapping("/times")
        public Payload json(@RequestBody Payload value) { return value; }
        @PostMapping("/times/form")
        public LocalDateTime form(@ModelAttribute TimeForm value) { return value.getAt(); }
    }
}
