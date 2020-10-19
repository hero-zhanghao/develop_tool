package com.easypay.service.user.aop;

import com.easypay.service.user.annotation.ScheduledLock;
import com.easypay.service.user.exception.RedisUpdateException;
import com.easypay.service.user.utils.RedisLockUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Slf4j
public class ScheduledLockAspect {

    private static final String KEY = "scheduled";

    @Pointcut("@annotation(com.easypay.service.user.annotation.ScheduledLock) && @annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void annotationPoinCut(){}

    @Around(value = "annotationPoinCut()")
    public Object doAround(ProceedingJoinPoint joinPoint){
        log.info("task lock aspect task start ... , method = {}" , joinPoint.getSignature().getName());
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method targetMethod = methodSignature.getMethod();
        final ScheduledLock scheduledLock = targetMethod.getAnnotation(ScheduledLock.class);
        RedisLockUtils.lockAndOperation(KEY,signature.getName(), Duration.of(scheduledLock.timeout(),scheduledLock.timeoutUnit()), Duration.of(scheduledLock.waiting(),scheduledLock.waitingUnit()), () ->
                {
                    try {
                        return Mono.justOrEmpty(joinPoint.proceed(joinPoint.getArgs()));
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
        ).subscribe(result -> {} , error -> { if (!(error instanceof RedisUpdateException)){log.error("executing error",error);}});
        log.info("task lock aspect task end... , method = {}" , joinPoint.getSignature().getName());
        return null;
    }



}
