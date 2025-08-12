package io.github.majianzheng.jarboot.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验
 * @author majianzheng
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrivilegeCheck {
    /** 权限码 */
    String[] value() default {};

    /** 是否忽略权限校验 */
    boolean ignore() default false;
}
