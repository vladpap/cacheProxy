package ru.sbt.annotations;

import java.lang.annotation.*;

import static ru.sbt.annotations.CacheType.IN_MEMORY;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {

    CacheType cacheType() default IN_MEMORY;

    String fileNamePrefix() default "";

    boolean zip() default false;

    Class[] identityBy() default {};

    int maxListList() default 100_000;
}