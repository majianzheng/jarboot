
package io.github.majianzheng.jarboot.exception;

import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author majianzheng
 */
@ControllerAdvice
public class JarbootExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarbootExceptionHandler.class);

    @ExceptionHandler({JwtException.class})
    public ResponseEntity<ResponseSimple> handleAuthException(JwtException e) {
        LOGGER.warn("认证错误: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(HttpResponseUtils.error(HttpStatus.UNAUTHORIZED.value(), "请登录"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseSimple> handleAccessDeniedException(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(HttpResponseUtils.error("当前角色没有权限！"));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseSimple> handleIllegalArgumentException(IllegalArgumentException e) {
        LOGGER.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(HttpResponseUtils.error(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(JarbootException.class)
    public ResponseEntity<ResponseSimple> handleJarbootException(JarbootException e) {
        LOGGER.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(HttpResponseUtils.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(JarbootRunException.class)
    public ResponseEntity<ResponseSimple> handleJarbootRunException(JarbootRunException e) {
        LOGGER.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(HttpResponseUtils.error(-1, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseSimple> handleException(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(HttpResponseUtils.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
    }
}
