package io.github.majianzheng.jarboot.api;

/**
 * Agent Service<br>
 * Use {@link JarbootFactory} to create
 * @author majianzheng
 */
public interface AgentService {
    /**
     * 服务启动完成，通知Jarboot Server已完成<br>
     * 注：引入了spring-boot-starter-jarboot的应用，忽略此接口，无需主动调用，会自动监控Spring Boot的生命周期<br>
     * 非Spring boot或未引入的应用，当没有主动调用该方法时，默认情况下是等待控制一段时间没有输出时判定为启动完成
     * 判定时间可通过VM参数：-Dstart.determine.time=5000 来指定，默认5000毫秒
     */
    void setStarted();

    /**
     * 向浏览器客户端发出提示信息
     * @param message 消息
     * @param sessionId 客户端会话id，若为null或空则广播所有客户端
     */
    void noticeInfo(String message, String sessionId);

    /**
     * 向浏览器客户端发出警告信息
     * @param message 消息
     * @param sessionId 客户端会话id，若为null或空则广播所有客户端
     */
    void noticeWarn(String message, String sessionId);

    /**
     * 向浏览器客户端发出错误信息
     * @param message 消息
     * @param sessionId 客户端会话id，若为null或空则广播所有客户端
     */
    void noticeError(String message, String sessionId);

    /**
     * 获取服务名
     * @return 服务名
     */
    String getServiceName();

    /**
     * 获取连接的Jarboot，如http://127.0.0.1:9899
     * @return Jarboot服务地址
     */
    String getJarbootHost();

    /**
     * 获取Jarboot的类加载器
     * @return Jarboot的类加载器
     */
    ClassLoader getJarbootClassLoader();
}
