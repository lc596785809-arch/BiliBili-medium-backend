package com.xypu.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    public static final String CHECK_CODE_CLIENT_PREFIX = "check_code:client:";
    public static final String CHECK_CODE_ADMIN_PREFIX = "check_code:admin:";
    public static final String AUTH_TOKEN_PREFIX = "auth:token:";

    public static final String REDIS_KEY_CATEGORY_LIST = "category:tree";
    public static final String REDIS_KEY_ROOT_CATEGORY_LIST = "category:root";

    /** 视频上传任务，value 为 UploadTaskVO 的 JSON，TTL 24h */
    public static final String UPLOAD_TASK_PREFIX = "upload:task:";
    /** 视频播放次数累计，value 为 Long 字符串，定期落库 */
    public static final String VIDEO_PLAY_PREFIX = "video:play:";
    /** 待删除视频目录队列（Set），转码期间取消上传时写入，定时任务负责清理磁盘 */
    public static final String VIDEO_DELETE_QUEUE = "video:delete:queue";

    public static final long CHECK_CODE_TTL = 300L;
    public static final long TOKEN_TTL = 604800L;
    public static final long TOKEN_REFRESH_THRESHOLD = 86400L;
    /** 分类缓存 24 小时，写操作时显式删除保证一致性 */
    public static final long CATEGORY_CACHE_TTL = 86400L;
    /** 上传任务缓存 24 小时 */
    public static final long UPLOAD_TASK_TTL = 86400L;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public void expire(String key, long ttlSeconds) {
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    /** 原子自增，用于播放次数等计数场景，返回自增后的值 */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /** 不设置 TTL 的永久存储（缓存由业务显式删除） */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 向 Set 中添加一个成员 */
    public void sAdd(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /** 从 Set 中移除一个成员 */
    public void sRem(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    /** 获取 Set 中所有成员 */
    public Set<String> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /** 判断成员是否在 Set 中 */
    public Boolean sIsMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }
}
