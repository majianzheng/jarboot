package com.mz.jarboot.auth.annotation;

import java.lang.annotation.*;

@Target({java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Permission {
    String value() default "";

    String role() default "";
}
