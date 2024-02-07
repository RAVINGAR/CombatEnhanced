package com.ravingarinc.combat.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods/constructors annotated with this means by default that they do use Bukkit API.
 * and as such should be called from a synchronous thread.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface BukkitApi {
    boolean usesApi() default true;
}
