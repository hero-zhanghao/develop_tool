package com.easypay.service.user.utils;

import com.easypay.service.user.exception.RedisUpdateException;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

@Component
@Slf4j
public class RedisLockUtils {

    private static ReactiveValueOperations valueOperations;

    private static final String RETRY_NUMBER = "retry_number";

    private static RedisConnectionFactory redisConnectionFactory;

    private static Map<String, RedisLockRegistry> REDIS_LOCK_REGISTRY_BY_KEY_MAP = Maps.newConcurrentMap();

    @Autowired
    public void setValueOperations(ReactiveValueOperations valueOperations) {
        com.easypay.service.user.utils.RedisLockUtils.valueOperations = valueOperations;
    }

    @Autowired
    public void setRedisConnectionFactory(RedisConnectionFactory redisConnectionFactory) {
        com.easypay.service.user.utils.RedisLockUtils.redisConnectionFactory = redisConnectionFactory;
    }

    public static <T> Mono<T> lockAndOperation(String key, Object lockKey, Supplier<Mono<T>> operation) {
        return lockAndOperation(key, lockKey, Duration.ofSeconds(10L), operation);
    }

    public static <T> Mono<T> lockAndOperation(String key, Object lockKey, Duration timeout, Supplier<Mono<T>> operation) {
        return lockAndOperation(key, lockKey, timeout, Duration.ofSeconds(2L),operation);
    }


    public static <T> Mono<T> lockAndOperation(String key, Object lockKey, Duration timeout, Duration waiting, Supplier<Mono<T>> operation) {
        final Lock obtain = getRedisLockRegistryByKey(key, timeout).obtain(lockKey);
//                        final Mono<Boolean> lockMono = valueOperations.setIfAbsent(StrUtil.format(GlobalRediskeyConstant.LOCK_KEY, key), 1,timeout);
        final Mono<T> result = Mono.just(0)
                .flatMap(ignore -> {
                    try {
                        if (obtain.tryLock(waiting.toMillis(), TimeUnit.MILLISECONDS)) {
                            final Long startTime = System.currentTimeMillis();
                            log.info("Got the distributed lock and executing , key  = {} , lockKey = {} , time = {} ", key, lockKey, LocalDateTime.now());
                            return operation.get().doFinally(o -> obtain.unlock());
                        } else {
                            log.info("The distributed lock was not obtained and execution is over, key = {} , lockKey = {}, time = {} ", key, lockKey, LocalDateTime.now());
                            return Mono.error(new RedisUpdateException());
                        }
                    } catch (InterruptedException e) {
                        log.info("The distributed lock was not obtained and execution is over, key = {} , lockKey = {} ,  time = {} ", key, lockKey, LocalDateTime.now());
                        return Mono.error(new RedisUpdateException());
                    }
                });
        return result;
//                        return lockMono.flatMap(b -> {
//                            if (b){
//                                return operation.get();
//                            }else{
//                                final Integer retryNumber = context.getOrDefault(RETRY_NUMBER, 1);
//                                if (retryNumber <= retryCount){
//                                    context.put(RETRY_NUMBER,retryNumber+1);
//                                    return Mono.delay(waitingRetry)
//                                            .then(lockAndOperation(key,timeout,retryCount,waitingRetry,operation));
//                                }
//                                return Mono.error(new RedisUpdateException());
//                            }
//                        });
//                    .flatMap(t -> valueOperations.delete(StrUtil.format(GlobalRediskeyConstant.LOCK_KEY, key)).map(b -> t));
}


    public static RedisLockRegistry getRedisLockRegistryByKey(String key, Duration timeout) {
        if (!REDIS_LOCK_REGISTRY_BY_KEY_MAP.containsKey(key)) {
            REDIS_LOCK_REGISTRY_BY_KEY_MAP.put(key, new RedisLockRegistry(redisConnectionFactory, key, timeout.toMillis()));
        }
        return REDIS_LOCK_REGISTRY_BY_KEY_MAP.get(key);
    }


}

