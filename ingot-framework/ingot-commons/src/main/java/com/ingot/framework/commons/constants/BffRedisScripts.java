package com.ingot.framework.commons.constants;

/**
 * <p>
 * BFF 与响应式网关共用的同槽原子安全状态脚本；JSON 载荷保持原字节，避免大整数精度损失。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class BffRedisScripts {

    private BffRedisScripts() {
    }

    /** 原子登记撤销载荷及可扫描到期索引。 */
    public static final String ENQUEUE_CLEANUP = """
            if redis.call('EXISTS',KEYS[3]) == 1 then return 0 end
            redis.call('HSET',KEYS[1],ARGV[1],ARGV[2])
            redis.call('ZADD',KEYS[2],ARGV[3],ARGV[1])
            return 1
            """;

    /** create transaction 的条件推进协议。 */
    public static final String CREATE_TRANSACTION = """
            if not redis.call('GET', KEYS[1]) then return -1 end
            redis.call('ZREMRANGEBYSCORE', KEYS[3], '-inf', ARGV[1])
            if redis.call('ZCARD', KEYS[3]) >= tonumber(ARGV[2]) then return -2 end
            if not redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[4], 'NX') then return 0 end
            redis.call('ZADD', KEYS[3], ARGV[5], ARGV[6])
            local ttl=redis.call('TTL',KEYS[1]); if ttl < tonumber(ARGV[4]) then redis.call('EXPIRE',KEYS[1],ARGV[4]) end
            local at=redis.call('TTL',KEYS[3]); if at < tonumber(ARGV[4]) then redis.call('EXPIRE',KEYS[3],ARGV[4]) end
            return 1
            """;

    /** cas transaction 的条件推进协议。 */
    public static final String CAS_TRANSACTION = """
            local raw=redis.call('GET',KEYS[1]); if not raw then return 0 end
            local t=cjson.decode(raw)
            if tostring(t.version) ~= ARGV[1] or t.stage ~= ARGV[2] then return 0 end
            if tonumber(t.expiresAt) <= tonumber(ARGV[3]) then return 0 end
            if not redis.call('GET',KEYS[3]) then return 0 end
            if ARGV[10] == '1' and (tonumber(t.leaseUntil) <= tonumber(ARGV[3]) or tonumber(ARGV[9]) <= tonumber(ARGV[3])) then return 0 end
            redis.call('SET',KEYS[1],ARGV[4],'EX',ARGV[5])
            if ARGV[6] == '1' then redis.call('ZREM',KEYS[2],t.transactionId) else redis.call('ZADD',KEYS[2],ARGV[11],t.transactionId) end
            if ARGV[7] ~= '' then redis.call('HSET',KEYS[4],ARGV[8],ARGV[7]); redis.call('ZADD',KEYS[5],ARGV[9],ARGV[8]) end
            return 1
            """;

    /** commit session 的条件推进协议。 */
    public static final String COMMIT_SESSION = """
            local raw=redis.call('GET',KEYS[1]); if not raw then return 0 end
            local t=cjson.decode(raw)
            if tostring(t.version) ~= ARGV[1] or t.stage ~= ARGV[2] then return 0 end
            if tonumber(t.expiresAt) <= tonumber(ARGV[3]) or tonumber(t.leaseUntil) <= tonumber(ARGV[3]) then return 0 end
            if not redis.call('GET',KEYS[5]) then return 0 end
            local prefix=ARGV[9]
            local function retire(id)
             if not id or id == '' or id == ARGV[6] then return end
             local key=prefix..'session:'..id
             local old=redis.call('GET',key)
             if old then
              local s=cjson.decode(old)
              local job=cjson.encode({sessionId=id,appId=s.appId,bindingId=s.bindingId,transactionId=s.transactionId,sid=s.sid,authCookie=s.authCookie,pendingDelivery=false})
              redis.call('HSET',KEYS[7],id,job); redis.call('ZADD',KEYS[8],ARGV[3],id)
              redis.call('DEL',key,prefix..'delivered:'..id)
              local current=prefix..'binding-current:'..s.bindingId
              if redis.call('GET',current) == id then redis.call('DEL',current) end
             end
            end
            retire(redis.call('GET',KEYS[3])); retire(ARGV[10])
            redis.call('SET',KEYS[2],ARGV[4],'EX',ARGV[5])
            redis.call('SET',KEYS[3],ARGV[6],'EX',ARGV[5])
            redis.call('SET',KEYS[1],ARGV[7],'EX',ARGV[8])
            redis.call('ZREM',KEYS[4],t.transactionId)
            local bt=redis.call('TTL',KEYS[5]); if bt < tonumber(ARGV[5]) then redis.call('EXPIRE',KEYS[5],ARGV[5]) end
            redis.call('HSET',KEYS[7],ARGV[6],ARGV[11]); redis.call('ZADD',KEYS[8],ARGV[12],ARGV[6])
            return 1
            """;

    /** read session 的条件推进协议。 */
    public static final String READ_SESSION = """
            local raw=redis.call('GET',KEYS[1]); if not raw then return nil end
            local s=cjson.decode(raw)
            if s.appId ~= ARGV[1] or tonumber(s.expiresAt) <= tonumber(ARGV[2]) then return nil end
            if redis.call('GET',ARGV[3]..'binding-current:'..s.bindingId) ~= s.sessionId then return nil end
            if redis.call('EXISTS',ARGV[3]..'delivered:'..s.sessionId) == 0 and tonumber(s.recoveryExpiresAt) <= tonumber(ARGV[2]) then return nil end
            return raw
            """;

    /** confirm session 的条件推进协议。 */
    public static final String CONFIRM_SESSION = """
            local raw=redis.call('GET',KEYS[1]); if not raw then return 0 end
            local s=cjson.decode(raw)
            if s.appId ~= ARGV[1] or tonumber(s.expiresAt) <= tonumber(ARGV[2]) then return 0 end
            if redis.call('GET',KEYS[3]) ~= s.sessionId then return 0 end
            if redis.call('EXISTS',KEYS[2]) == 0 and tonumber(s.recoveryExpiresAt) <= tonumber(ARGV[2]) then return 0 end
            redis.call('SET',KEYS[2],'1','EX',math.max(1,tonumber(s.expiresAt)-tonumber(ARGV[2])))
            return 1
            """;

    /** invalidate session 的条件推进协议。 */
    public static final String INVALIDATE_SESSION = """
            local raw=redis.call('GET',KEYS[1]); if not raw then return 0 end
            local s=cjson.decode(raw)
            local job=cjson.encode({sessionId=s.sessionId,appId=s.appId,bindingId=s.bindingId,transactionId=s.transactionId,sid=s.sid,authCookie=s.authCookie,pendingDelivery=false})
            redis.call('HSET',KEYS[4],s.sessionId,job); redis.call('ZADD',KEYS[5],ARGV[1],s.sessionId)
            redis.call('DEL',KEYS[1],KEYS[2])
            if redis.call('GET',KEYS[3]) == s.sessionId then redis.call('DEL',KEYS[3]) end
            return 1
            """;

    /** prepare cleanup 的条件推进协议。 */
    public static final String PREPARE_CLEANUP = """
            if redis.call('GET',KEYS[7]) ~= ARGV[1] then return 0 end
            local raw=redis.call('HGET',KEYS[1],ARGV[2]); if not raw then return 0 end
            local job=cjson.decode(raw)
            if job.pendingDelivery and redis.call('EXISTS',KEYS[3]) == 1 then
             redis.call('HDEL',KEYS[1],ARGV[2]); redis.call('ZREM',KEYS[2],ARGV[2]); return 0
            end
            redis.call('DEL',KEYS[4],KEYS[3])
            if redis.call('GET',KEYS[5]) == ARGV[2] then redis.call('DEL',KEYS[5]) end
            redis.call('HSET',KEYS[1],ARGV[2],ARGV[3])
            return 1
            """;

    /** finish cleanup 的条件推进协议。 */
    public static final String FINISH_CLEANUP = """
            if redis.call('GET',KEYS[3]) ~= ARGV[1] then return 0 end
            redis.call('HDEL',KEYS[1],ARGV[2]); redis.call('ZREM',KEYS[2],ARGV[2]); redis.call('DEL',KEYS[3]); return 1
            """;

}
