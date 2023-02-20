package com.tenio.core.bootstrap.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @since 0.5.0
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface ClientCommand {

  short value();
}