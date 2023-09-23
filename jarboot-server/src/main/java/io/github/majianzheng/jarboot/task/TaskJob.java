package io.github.majianzheng.jarboot.task;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.base.AgentManager;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import io.github.majianzheng.jarboot.utils.PropertyFileUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务Job
 * @author mazheng
 */
public class TaskJob extends QuartzJobBean {
    private static final Map<String, ServiceSetting> STARTING_MAP = new ConcurrentHashMap<>(16);
    @Override
    protected void executeInternal(JobExecutionContext context) {
        final String sid = context.getMergedJobDataMap().getString(CommonConst.SID_PARAM);
        final String userDir = context.getMergedJobDataMap().getString(CommonConst.USER_DIR);
        final String name = context.getMergedJobDataMap().getString(CommonConst.SERVICE_NAME_PARAM);
        TaskUtils.getTaskExecutor().execute(() -> startTask(sid, userDir, name));
    }

    private void startTask(String sid, String userDir, String name) {
        if (AgentManager.getInstance().exist(sid)) {
            MessageUtils.info(name + "正在运行中或启动中，定时任务跳过！");
            MessageUtils.console(sid, "正在运行中或启动中，无需再次执行！");
            return;
        }
        MessageUtils.console(sid, "定时任务触发，开始执行...");
        try {
            ServiceSetting setting = PropertyFileUtils.getServiceSetting(userDir, name);
            if (null != STARTING_MAP.putIfAbsent(sid, setting)) {
                MessageUtils.info(name + "正在启动中，定时任务跳过！");
                MessageUtils.console(sid, "正在启动中，无需再次执行！");
                return;
            }
            //记录开始时间
            long startTime = System.currentTimeMillis();
            TaskUtils.startService(setting);
            double costTime = (System.currentTimeMillis() - startTime)/1000.0f;
            String msg = String.format("定时任务\033[96;1m%s\033[0m 启动耗时 \033[91;1m%.3f\033[0m second.\033[5m✨\033[0m", name, costTime);
            MessageUtils.console(sid, msg);
        } catch (Exception e) {
            MessageUtils.console(sid, "启动失败：" + e.getMessage());
            MessageUtils.printException(sid, e);
        } finally {
            STARTING_MAP.remove(sid);
        }
    }
}
