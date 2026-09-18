package com.ingot.cloud.bff.service;

import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 登录事务 Redis 存储。
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class LoginTransactionService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final BffProperties properties;

    public void save(LoginTransaction transaction) {
        long ttl = Math.max(1, transaction.getExpiresAt() - (System.currentTimeMillis() / 1000));
        try {
            redisTemplate.opsForValue().set(
                    CacheConstants.bffLoginTransactionKey(transaction.getTransactionId()),
                    objectMapper.writeValueAsString(transaction),
                    ttl,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("保存登录事务失败", exception);
        }
    }

    public LoginTransaction require(String transactionId) {
        if (StrUtil.isBlank(transactionId)) {
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
        }
        String json = redisTemplate.opsForValue().get(CacheConstants.bffLoginTransactionKey(transactionId));
        if (StrUtil.isBlank(json)) {
            throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
        }
        try {
            LoginTransaction transaction = objectMapper.readValue(json, LoginTransaction.class);
            if (transaction.getExpiresAt() <= System.currentTimeMillis() / 1000) {
                redisTemplate.delete(CacheConstants.bffLoginTransactionKey(transactionId));
                throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
            }
            return transaction;
        } catch (JsonProcessingException exception) {
            throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
        }
    }

    public void delete(String transactionId) {
        if (StrUtil.isNotBlank(transactionId)) {
            redisTemplate.delete(CacheConstants.bffLoginTransactionKey(transactionId));
        }
    }

    public long ttlSeconds() {
        return properties.getTransactionTtl();
    }

    public void saveBinding(AuthBinding binding, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    CacheConstants.bffAuthBindingKey(binding.getBindingId()),
                    objectMapper.writeValueAsString(binding),
                    ttlSeconds,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("保存浏览器绑定失败", exception);
        }
    }

    /**
     * 删除 Redis 中的浏览器绑定。{@code bindingId} 为空时不访问 Redis。
     *
     * @param bindingId 绑定 ID，可空
     */
    public void deleteBinding(String bindingId) {
        if (StrUtil.isNotBlank(bindingId)) {
            redisTemplate.delete(CacheConstants.bffAuthBindingKey(bindingId));
        }
    }

    public AuthBinding requireBinding(String bindingId) {
        if (StrUtil.isBlank(bindingId)) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        String json = redisTemplate.opsForValue().get(CacheConstants.bffAuthBindingKey(bindingId));
        if (StrUtil.isBlank(json)) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        try {
            return objectMapper.readValue(json, AuthBinding.class);
        } catch (JsonProcessingException exception) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
    }

    public void saveTicket(String ticket, String transactionId, long ttlSeconds) {
        redisTemplate.opsForValue().set(ticketKey(ticket), transactionId, ttlSeconds, TimeUnit.SECONDS);
    }

    public String requireTransactionIdByTicket(String ticket) {
        if (StrUtil.isBlank(ticket)) {
            throw new BffAuthException(BffErrorCode.TICKET_INVALID);
        }
        String transactionId = redisTemplate.opsForValue().get(ticketKey(ticket));
        if (StrUtil.isBlank(transactionId)) {
            throw new BffAuthException(BffErrorCode.TICKET_INVALID);
        }
        return transactionId;
    }

    public void deleteTicket(String ticket) {
        if (StrUtil.isNotBlank(ticket)) {
            redisTemplate.delete(ticketKey(ticket));
        }
    }

    private static String ticketKey(String ticket) {
        return CacheConstants.BFF_LOGIN_TRANSACTION + ":ticket:" + ticket;
    }
}
