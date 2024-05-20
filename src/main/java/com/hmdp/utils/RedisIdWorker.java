package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author 韩飞龙
 * @version 1.0
 */
@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final int COUNT_BITS =32;
    public Long nextId(String keyPrefix){

        LocalDateTime now = LocalDateTime.now();
        long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowEpochSecond - BEGIN_TIMESTAMP;

        //2生成序列号
        //1获取当前日前
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //返回自增id
        long count = stringRedisTemplate.opsForValue().increment("icr" + keyPrefix + date);

        return timestamp<<COUNT_BITS | count;

    }
}
