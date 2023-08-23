package io.github.majianzheng.jarboot.common.utils;

import io.github.majianzheng.jarboot.common.pojo.ResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * http响应内容封装工具类
 * @author mazheng
 */
public class HttpResponseUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtils.class);

    private HttpResponseUtils() {
    }

    public static <T> ResponseVo<T> setResponseVo(T obj, int code, String msg) {
        ResponseVo<T> responseVo = new ResponseVo<>();
        responseVo.setMsg(msg);
        responseVo.setCode(code);
        responseVo.setData(obj);
        responseVo.setSuccess(0 == code);
        return responseVo;
    }

    public static <T> ResponseVo<T> success() {
        return setResponseVo(null, 0, null);
    }

    public static <T> ResponseVo<T> success(T data) {
        return setResponseVo(data, 0, null);
    }

    public static <T> ResponseVo<T> error(String msg, Exception e) {
        logger.error(msg, e);
        return setResponseVo(null, -1, msg);
    }

    public static <T> ResponseVo<T> error(String msg) {
        logger.error(msg);
        return setResponseVo(null, -1, msg);
    }

    public static <T> ResponseVo<T> error(Exception e) {
        logger.error("error:", e);
        return setResponseVo(null, -1, exception2string(e));
    }

    public static <T> ResponseVo<T> error(Integer code, String msg) {
        logger.error(msg);
        return setResponseVo(null, code, msg);
    }

    public static <T> ResponseVo<T> error() {
        return error("未知异常");
    }


    private static String exception2string(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionStr = "\r\n" + sw.toString() + "\r\n";
            sw.close();
            pw.close();
            return exceptionStr;
        } catch (Exception var4) {
            return "ErrorInfoFromException";
        }
    }
}
