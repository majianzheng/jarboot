package io.github.majianzheng.jarboot.aspect;

import io.github.majianzheng.jarboot.common.AuditArgsFormat;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.annotation.EnableAuditLog;
import io.github.majianzheng.jarboot.common.annotation.PrivilegeCheck;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.constant.AuthConst;
import io.github.majianzheng.jarboot.dao.AuditLogDao;
import io.github.majianzheng.jarboot.entity.AuditLog;
import io.github.majianzheng.jarboot.service.PrivilegeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * 系统日志，切面处理类
 *
 * @author mazheng
 */
@Aspect
@Component
public class CustomAspect {

	@Resource
	private AuditLogDao auditLogDao;
	@Resource
	private PrivilegeService privilegeService;
	
	@Pointcut("@annotation(io.github.majianzheng.jarboot.common.annotation.EnableAuditLog)")
	public void logPointCut() {
		// ignore
	}

	@Pointcut("@annotation(io.github.majianzheng.jarboot.common.annotation.PrivilegeCheck)")
	public void privilegeCheckPointCut() {
		// ignore
	}

	@Before("privilegeCheckPointCut()")
	public void checkPrivilege(JoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		PrivilegeCheck privilegeCheck = method.getAnnotation(PrivilegeCheck.class);
		if(privilegeCheck == null || privilegeCheck.value().length == 0) {
			return;
		}
		Set<String> roles = CommonUtils.getLoginRoles();
		if (roles == null || roles.isEmpty()) {
			throw new JarbootException("当前用户未赋予角色！");
		}
		if (roles.contains(AuthConst.SYS_ROLE)) {
			return;
		}
		String[] authCodes = privilegeCheck.value();
		for (String role : roles) {
			check(role, authCodes);
		}
	}

	private void check(String role, String[] authCodes) {
		for (String code : authCodes) {
			boolean hasPrivilege = privilegeService.hasPrivilege(role, code);
			if (hasPrivilege) {
				return;
			}
		}
		throw new JarbootException("当前角色无权限访问！");
	}

	@AfterReturning("logPointCut()")
	public void around(JoinPoint joinPoint) {
		//保存日志
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		AuditLog sysLog = new AuditLog();
		EnableAuditLog syslog = method.getAnnotation(EnableAuditLog.class);
		if(syslog == null){
			return;
		}
		String username = CommonUtils.getLoginUsername();
		if (StringUtils.isEmpty(username)) {
			return;
		}
		sysLog.setCreateTime(System.currentTimeMillis());
		//注解上的描述
		sysLog.setOperation(syslog.value());

		//请求的方法名
		String methodName = signature.getName();
		if (null != method.getAnnotation(GetMapping.class)) {
			methodName = "GET";
		} else if (null != method.getAnnotation(PostMapping.class)) {
			methodName = "POST";
		} else if (null != method.getAnnotation(DeleteMapping.class)) {
			methodName = "DELETE";
		} else if (null != method.getAnnotation(PutMapping.class)) {
			methodName = "PUT";
		} else {
			methodName = StringUtils.unqualify(methodName);
		}
		sysLog.setMethod(methodName);
		//请求的参数
		Object[] args = joinPoint.getArgs();
		String arg = formatArgs(args, syslog.argsFormat());
		sysLog.setArgument(arg);
		RequestAttributes requestAttr = RequestContextHolder.getRequestAttributes();
		if (requestAttr instanceof ServletRequestAttributes) {
			//获取request
			HttpServletRequest request = ((ServletRequestAttributes)requestAttr).getRequest();
			//设置IP地址
			sysLog.setRemoteIp(CommonUtils.getActualIpAddr(request));
		}
		sysLog.setUsername(username);
		//保存系统日志
		auditLogDao.save(sysLog);
	}

	private String formatArgs(Object[] args, Class<? extends AuditArgsFormat> cls) {
		try {
			AuditArgsFormat formater = cls.getConstructor().newInstance();
            return formater.format(args);
		} catch (Exception e) {
			// ignore
		}
		return StringUtils.EMPTY;
	}
}
