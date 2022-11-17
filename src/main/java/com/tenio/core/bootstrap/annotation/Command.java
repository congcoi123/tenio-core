package com.tenio.core.bootstrap.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface Command {

  String label() default "";

  String[] usage() default {""};

  String description() default "";

  boolean isBackgroundRunning() default false;
}
