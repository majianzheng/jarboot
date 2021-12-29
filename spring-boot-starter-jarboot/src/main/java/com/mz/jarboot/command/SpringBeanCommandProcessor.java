package com.mz.jarboot.command;

import com.mz.jarboot.api.cmd.annotation.*;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 查看Spring bean信息命令处理器
 * @author majianzheng
 */
@Name("spring.bean")
@Summary("View Spring Bean info")
@Description("\nEXAMPLES:\n spring.bean\n spring.bean -b beanName\n" +
        " spring.bean -b beanName -d\n")
public class SpringBeanCommandProcessor implements CommandProcessor {
    private static final String CGLIG_FLAG = "$$EnhancerBySpringCGLIB$$";
    private final ApplicationContext context;
    private String beanName;
    private boolean showDetail;

    public SpringBeanCommandProcessor(ApplicationContext context) {
        this.context = context;
    }

    @Option(shortName = "b", longName = "bean")
    @Description("The spring bean name")
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Option(shortName = "d", longName = "detail", flag = true)
    @Description("Enable show bean detail info or not.")
    @DefaultValue("false")
    public void setRegEx(boolean showDetail) {
        this.showDetail = showDetail;
    }

    @Override
    public String process(CommandSession session, String[] args) {
        if (null == beanName || beanName.isEmpty()) {
            String[] beans = context.getBeanDefinitionNames();
            session.console("\033[32;1mAll spring bean definition names:\033[0m");
            for (String bean : beans) {
                session.console(bean);
            }
            session.console("<br>spring bean total count: " + beans.length);
            return "";
        }
        Object bean = context.getBean(beanName);
        Class<?> beanClass = bean.getClass();
        String className = beanClass.getName();
        if (className.contains(CGLIG_FLAG)) {
            beanClass = beanClass.getSuperclass();
            className = beanClass.getName();
        }
        session.console("bean class: " + className);
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length > 0) {
            session.console("implements interfaces:");
            for (int i = 0; i < interfaces.length; ++i) {
                session.console("[" + i + "]. " + interfaces[i]);
            }
        }

        if (showDetail) {
            //打印成员详细信息
            Field[] fields = beanClass.getDeclaredFields();
            session.console("<br>Bean fields:");
            for (Field field : fields) {
                String name = field.getName();
                session.console(name + " : " + field.getType());
            }
            session.console("<br>public method:");
            Method[] methods = beanClass.getMethods();
            for (Method method : methods) {
                session.console(method.getName());
            }
        }
        return "";
    }

    @Override
    public void afterProcess(String result, Throwable e) {
        //重置
        this.beanName = null;
        this.showDetail = false;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
