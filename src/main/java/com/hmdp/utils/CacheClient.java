package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author 韩飞龙
 * @version 1.0
 */
@Component
@Slf4j
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public void set(String key, Object value, Long time , TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);

    }
    public void setWithLogicalExpire(String key, Object value, Long time , TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
      redisData.setExpireTime( LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData),time,unit);
    }
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time , TimeUnit unit){
        String key = keyPrefix+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJson)) {
            //转换成对象

            return JSONUtil.toBean(shopJson, type);
        }
        //返回的是“”空字符串
        if(shopJson!=null){
            return null;
        }


        R r = dbFallback.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
       this.set(key,r,time,unit);
        return r;
    }

    private static  final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R  queryWithLogicalExpire(String preKeyfix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time , TimeUnit unit){
        String key = preKeyfix+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(shopJson)) {
            //转换成对象
            return null;
        }
        //返回的是“”空字符串
        //转换成Bean
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        //获取JSON字符串中的data数据
        JSONObject data =(JSONObject) redisData.getData();
        //转换成shop类型
        R r = JSONUtil.toBean(data,type);
        //获取过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        //未过期
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY+id;

        boolean islock = tryLock(lockKey);
        if(islock){
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                   dbFallback.apply(id);
                   this.setWithLogicalExpire(key,r,time,unit);
                }catch (Exception e){
                    throw  new RuntimeException();
                }
                finally {
                    delLock(lockKey);
                }
            });
        }


        return r;
    }
    private  boolean tryLock(String key){
//        具体来说，SETNX 命令的行为如下：
//        如果键不存在，SETNX 会设置键并返回 1。
//        如果键已经存在，SETNX 不会做任何操作并返回 0。
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return    BooleanUtil.isTrue(flag);
    }

    private  void delLock(String key){
        stringRedisTemplate.delete(key);
    }
}
