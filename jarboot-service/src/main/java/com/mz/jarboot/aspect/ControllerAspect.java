package com.mz.jarboot.aspect;

import com.mz.jarboot.aop.annotation.Permission;
import com.mz.jarboot.common.MzException;
import com.mz.jarboot.common.ResponseSimple;
import com.mz.jarboot.common.ResultCodeConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
@SuppressWarnings("all")
public class ControllerAspect {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    //切入所有controller的public方法
    @Pointcut("execution(public * com.mz.jarboot.controller.*.*(..))")
    public void pointcut(){
        //
    }

    private void checkPermit() {
        RequestAttributes obj = RequestContextHolder.getRequestAttributes();
        if (obj instanceof ServletRequestAttributes) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) obj;
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("token");
            //todo 校验token
        }
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) {
        Object obj;
        if (proceedingJoinPoint.getSignature() instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
            Method method = signature.getMethod();
            Permission permission = method.getAnnotation(Permission.class);
            if (null != permission) {
                //todo jwt鉴权（待实现），为了不引入过于庞大沉重的spring security，此处使用aop简易实现
                checkPermit();
            }
        }

        try {
            obj = proceedingJoinPoint.proceed();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
            //捕捉service层抛出的异常，包裹错误信息返回前端
            if (e instanceof MzException) {
                MzException e1 = (MzException)e;
                obj = new ResponseSimple(e1.getErrorCode(), e1.getMessage());
            } else {
                obj = new ResponseSimple(ResultCodeConst.INTERNAL_ERROR, e.getMessage());
            }
        }
        return obj;
    }
}
