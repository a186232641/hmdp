package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        String key = RedisConstants.CACHE_SHOP_TYPE;
        List<String> shopTypes;
        //1.先查询缓存
        shopTypes = stringRedisTemplate.opsForList().range(key, 0, -1);
        //2.缓存为空
        if(shopTypes.isEmpty()){
            List<ShopType> shopType;
            shopType = query().orderByAsc("sort").list();
            List<String> collect = shopType.stream().map(shopType1 -> JSONUtil.toJsonStr(shopType1)).collect(Collectors.toList());
            stringRedisTemplate.opsForList().leftPushAll(key,collect);
            stringRedisTemplate.expire(key,RedisConstants.CACHE_SHOP_TTL, TimeUnit.DAYS);
            return Result.ok(shopType);
        }
        List<ShopType> collect = shopTypes.stream()
                        .map(s -> JSONUtil.toBean(s, ShopType.class))
                                .collect(Collectors.toList());
        return Result.ok(collect);
    }
}
