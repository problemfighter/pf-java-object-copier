package com.problemfighter.java.oc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataMappingInfo {
    public boolean isStrict() default false;
    public String name() default "anonymous";
    public Class<?> customProcessor() default void.class;
}
