package com.ingot.framework.vc.config;

import com.anji.captcha.model.common.Const;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.Base64Utils;
import com.anji.captcha.util.ImageUtils;
import com.anji.captcha.util.StringUtils;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCGenerator;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProcessor;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProvider;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCPreChecker;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import com.ingot.framework.vc.module.servlet.VCProvider;
import com.ingot.framework.vc.properties.ImageCodeProperties;
import com.ingot.framework.vc.properties.InVCProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.FileCopyUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * <p>Description  : CaptchaConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 3:16 PM.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class CaptchaConfig {

    @Bean(name = "AjCaptchaCacheService")
    public CaptchaCacheService captchaCacheService(InVCProperties properties) {
        return CaptchaServiceFactory.getCache(properties.getImage().getCacheType().name());
    }

    @Bean
    public CaptchaService captchaService(InVCProperties properties) {
        ImageCodeProperties prop = properties.getImage();
        Properties config = new Properties();
        config.put(Const.CAPTCHA_CACHETYPE, prop.getCacheType().name());
        config.put(Const.CAPTCHA_WATER_MARK, prop.getWaterMark());
        config.put(Const.CAPTCHA_FONT_TYPE, prop.getFontType());
        config.put(Const.CAPTCHA_TYPE, prop.getType().getCodeValue());
        config.put(Const.CAPTCHA_INTERFERENCE_OPTIONS, prop.getInterferenceOptions());
        config.put(Const.ORIGINAL_PATH_JIGSAW, prop.getJigsaw());
        config.put(Const.ORIGINAL_PATH_PIC_CLICK, prop.getPicClick());
        config.put(Const.CAPTCHA_SLIP_OFFSET, prop.getSlipOffset());
        config.put(Const.CAPTCHA_AES_STATUS, String.valueOf(prop.getAesStatus()));
        config.put(Const.CAPTCHA_WATER_FONT, prop.getWaterFont());
        config.put(Const.CAPTCHA_CACAHE_MAX_NUMBER, prop.getCacheNumber());
        config.put(Const.CAPTCHA_TIMING_CLEAR_SECOND, prop.getTimingClear());

        config.put(Const.HISTORY_DATA_CLEAR_ENABLE, prop.isHistoryDataClearEnable() ? "1" : "0");

        config.put(Const.REQ_FREQUENCY_LIMIT_ENABLE, prop.isReqFrequencyLimitEnable() ? "1" : "0");
        config.put(Const.REQ_GET_LOCK_LIMIT, String.valueOf(prop.getReqGetLockLimit()));
        config.put(Const.REQ_GET_LOCK_SECONDS, String.valueOf(prop.getReqGetLockSeconds()));
        config.put(Const.REQ_GET_MINUTE_LIMIT, String.valueOf(prop.getReqGetMinuteLimit()));
        config.put(Const.REQ_CHECK_MINUTE_LIMIT, String.valueOf(prop.getReqCheckMinuteLimit()));
        config.put(Const.REQ_VALIDATE_MINUTE_LIMIT, String.valueOf(prop.getReqVerifyMinuteLimit()));

        config.put(Const.CAPTCHA_FONT_SIZE, String.valueOf(prop.getFontSize()));
        config.put(Const.CAPTCHA_FONT_STYLE, String.valueOf(prop.getFontStyle()));
        config.put(Const.CAPTCHA_WORD_COUNT, String.valueOf(prop.getClickWordCount()));

        if ((StringUtils.isNotBlank(prop.getJigsaw()) && prop.getJigsaw().startsWith("classpath:"))
                || (StringUtils.isNotBlank(prop.getPicClick()) && prop.getPicClick().startsWith("classpath:"))) {
            //自定义resources目录下初始化底图
            config.put(Const.CAPTCHA_INIT_ORIGINAL, "true");
            initializeBaseMap(prop.getJigsaw(), prop.getPicClick());
        }
        return CaptchaServiceFactory.getInstance(config);
    }

    private static void initializeBaseMap(String jigsaw, String picClick) {
        ImageUtils.cacheBootImage(getResourcesImagesFile(jigsaw + "/original/*.png"),
                getResourcesImagesFile(jigsaw + "/slidingBlock/*.png"),
                getResourcesImagesFile(picClick + "/*.png"));
    }

    public static Map<String, String> getResourcesImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String string = Base64Utils.encodeToString(bytes);
                String filename = resource.getFilename();
                imgMap.put(filename, string);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgMap;
    }

    @Bean(VCConstants.BEAN_NAME_GENERATOR_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_GENERATOR_IMAGE})
    public VCGenerator imageGenerator(InVCProperties properties) {
        return new DefaultCaptchaVCGenerator(properties.getImage());
    }

    @Bean(VCConstants.BEAN_NAME_SEND_CHECKER_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_SEND_CHECKER_IMAGE})
    public VCPreChecker imageSendChecker(RedisTemplate<String, Object> redisTemplate,
                                         InVCProperties properties) {
        return new DefaultCaptchaVCPreChecker(redisTemplate, properties.getImage());
    }

    @Bean(VCConstants.BEAN_NAME_PROVIDER_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROVIDER_IMAGE})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProvider imageProvider(CaptchaService captchaService) {
        return new DefaultCaptchaVCProvider(captchaService);
    }

    @Bean(VCConstants.BEAN_NAME_PROCESSOR_IMAGE)
    @ConditionalOnMissingBean(name = {VCConstants.BEAN_NAME_PROCESSOR_IMAGE})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public VCProcessor imageProcessor(CaptchaService captchaService) {
        return new DefaultCaptchaVCProcessor(captchaService);
    }
}
