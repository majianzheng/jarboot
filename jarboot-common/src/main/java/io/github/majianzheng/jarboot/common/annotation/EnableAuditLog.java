package io.github.majianzheng.jarboot.common.annotation;

import io.github.majianzheng.jarboot.common.AuditArgsFormat;
import io.github.majianzheng.jarboot.common.DefaultAuditArgsFormat;

import java.lang.annotation.*;

/**
 * 系统日志注解
 *
 * @author mazheng
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableAuditLog {
	String value() default "";
	Class<? extends AuditArgsFormat> argsFormat() default DefaultAuditArgsFormat.class;
}
