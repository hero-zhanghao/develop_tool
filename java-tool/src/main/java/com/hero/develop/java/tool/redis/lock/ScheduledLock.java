package com.easypay.service.user.annotation;

import java.lang.annotation.*;
import java.time.temporal.ChronoUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ScheduledLock {

    long timeout() default 59L;

    ChronoUnit timeoutUnit() default  ChronoUnit.SECONDS;

    long waiting() default 500L;

    ChronoUnit waitingUnit() default  ChronoUnit.MILLIS;

}
