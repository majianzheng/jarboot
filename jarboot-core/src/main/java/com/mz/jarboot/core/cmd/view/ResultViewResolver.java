package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.ResultModel;
import com.mz.jarboot.core.constant.CoreConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Result view resolver
 * @author jianzhengma
 */
@SuppressWarnings("all")
public class ResultViewResolver {
    private static final Logger logger = LoggerFactory.getLogger(CoreConstant.LOG_NAME);
    private Map<Class<?>, ResultView<? extends ResultModel>> resultViewMap = new ConcurrentHashMap<>();

    public ResultViewResolver() {
        initResultViews();
    }

    /**
     * 需要调用此方法初始化注册ResultView
     */
    private void initResultViews() {
        registerView(RowAffectView.class);

        //基本命令
        registerView(JvmView.class);
        registerView(SysPropView.class);

        //klass
        registerView(ClassLoaderView.class);
        registerView(DumpClassView.class);
        //registerView(GetStaticView.class);
        registerView(JadView.class);
//        registerView(MemoryCompilerView.class);
//        registerView(OgnlView.class);
//        registerView(RedefineView.class);
//        registerView(RetransformView.class);
//        registerView(SearchClassView.class);
//        registerView(SearchMethodView.class)


        //监控
        registerView(DashboardView.class);
        registerView(JvmView.class);
        //registerView(MBeanView.class);
        //registerView(PerfCounterView.class);
        registerView(ThreadView.class);
        //registerView(ProfilerView.class);
        registerView(EnhancerView.class);
        //registerView(MonitorView.class);
        //registerView(StackView.class);
        //registerView(TimeTunnelView.class);
        registerView(TraceView.class);
        registerView(WatchView.class);
    }

    public ResultView getResultView(ResultModel model) {
        return resultViewMap.get(model.getClass());
    }

    public ResultViewResolver registerView(Class modelClass, ResultView view) {
        this.resultViewMap.put(modelClass, view);
        return this;
    }

    public ResultViewResolver registerView(ResultView view) {
        Class modelClass = getModelClass(view);
        if (modelClass == null) {
            throw new NullPointerException("model class is null");
        }
        return this.registerView(modelClass, view);
    }

    public void registerView(Class<? extends ResultView> viewClass) {
        ResultView view = null;
        try {
            view = viewClass.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException("create view instance failure, viewClass:" + viewClass, e);
        }
        this.registerView(view);
    }

    /**
     * Get model class of result view
     *
     * @return
     */
    public static <V extends ResultView> Class getModelClass(V view) {
        //类反射获取子类的render方法第一个参数的ResultModel具体类型
        Class<? extends ResultView> viewClass = view.getClass();
        Method[] methods = viewClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals("render")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1
                        && parameterTypes[0] != ResultModel.class
                        && ResultModel.class.isAssignableFrom(parameterTypes[0])) {
                    logger.info("key:{}, value:{}", parameterTypes[0], viewClass);
                    return parameterTypes[0];
                }
            }
        }
        return null;
    }
}
