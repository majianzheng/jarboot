package io.github.majianzheng.jarboot.cluster;

import io.github.majianzheng.jarboot.api.pojo.ServiceGroup;
import io.github.majianzheng.jarboot.api.pojo.ServiceSetting;
import io.github.majianzheng.jarboot.api.pojo.SimpleInstance;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author mazheng
 */
@Component
public class ClusterClientProxy {
    private static final Logger logger = LoggerFactory.getLogger(ClusterClientProxy.class);

    @Autowired
    private ServiceManager serviceManager;
    @Autowired
    private SettingService settingService;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            4, 128, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8),
            JarbootThreadFactory.createThreadFactory("task.s-", true));

    public List<ServiceGroup> getServiceGroup() {
        List<ServiceGroup> groups = new ArrayList<>();
        ServiceGroup localGroup = serviceManager.getServiceGroup();
        groups.add(localGroup);
        // 获取集群其它服务器的组信息
        if (!ClusterClientManager.getInstance().isEnabled()) {
            return groups;
        }
        forEachOnlineClient(client -> groups.add(client.getServiceGroup()));
        return groups;
    }

    public List<ServiceGroup> getJvmGroup() {
        List<ServiceGroup> groups = new ArrayList<>();
        ServiceGroup localGroup = serviceManager.getJvmGroup();
        groups.add(localGroup);
        // 获取集群其它服务器的组信息
        if (!ClusterClientManager.getInstance().isEnabled()) {
            return groups;
        }
        forEachOnlineClient(client -> groups.add(client.getJvmGroup()));
        return groups;
    }

    public void startService(List<SimpleInstance> services) {
        List<ServiceSetting> settingList = services
                .stream()
                .map(this::getServiceSetting)
                .collect(Collectors.toList());
        TaskUtils.getTaskExecutor().execute(() -> startServiceSync(settingList));
    }

    public void stopService(List<SimpleInstance> services) {
        List<ServiceSetting> settingList = services
                .stream()
                .map(this::getServiceSetting)
                .collect(Collectors.toList());
        TaskUtils.getTaskExecutor().execute(() -> stopServiceSync(settingList));
    }

    public void restartService(List<SimpleInstance> services) {
        List<ServiceSetting> settingList = services
                .stream()
                .map(this::getServiceSetting)
                .collect(Collectors.toList());
        TaskUtils.getTaskExecutor().execute(() -> {
            //先依次终止
            stopServiceSync(settingList);
            //再依次启动
            startServiceSync(settingList);
        });
    }

    public void attach(String host, String pid) {
        if (needProxy(host)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(host);
            client.attach(pid);
        } else {
            serviceManager.attach(pid);
        }
    }

    public void deleteService(SimpleInstance instance) {
        if (needProxy(instance.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(instance.getHost());
            client.deleteService(instance.getName());
        } else {
            serviceManager.deleteService(instance.getHost());
        }
    }

    /**
     * 启动单个服务
     * @param setting 服务配置
     */
    public void startSingleService(ServiceSetting setting) {
        if (needProxy(setting.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(setting.getHost());
            final int maxWait = SettingUtils.getSystemSetting().getMaxStartTime() + 5000;
            String resp = client.requestSync(ClusterEventName.START_SERVICE, JsonUtils.toJsonString(setting), maxWait);
            if (StringUtils.isNotEmpty(resp)) {
                logger.error(resp);
            }
        } else {
            serviceManager.startSingleService(setting);
        }
    }

    /**
     * 停止单个服务
     * @param setting
     */
    public void stopSingleService(ServiceSetting setting) {
        if (needProxy(setting.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(setting.getHost());
            final int maxWait = SettingUtils.getSystemSetting().getMaxExitTime() + 5000;
            String resp = client.requestSync(ClusterEventName.STOP_SERVICE, JsonUtils.toJsonString(setting), maxWait);
            if (StringUtils.isNotEmpty(resp)) {
                logger.error(resp);
            }
        } else {
            serviceManager.stopSingleService(setting);
        }
    }

    public ServiceSetting getServiceSetting(SimpleInstance instance) {
        if (needProxy(instance.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(instance.getHost());
            if (null == client) {
                throw new JarbootException("集群实例不存在，host:"  + instance.getHost());
            }
            return client.getServiceSetting(instance.getName());
        } else {
            return settingService.getServiceSetting(instance.getName());
        }
    }

    private void startServiceSync(List<ServiceSetting> settingList) {
        PriorityQueue<ServiceSetting> priorityQueue = new PriorityQueue<>((o1, o2) -> o2.getPriority() - o1.getPriority());
        priorityQueue.addAll(settingList);

        ArrayList<ServiceSetting> taskList = new ArrayList<>();
        ServiceSetting setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServiceSetting next = priorityQueue.peek();
            if (null != next && !next.getPriority().equals(setting.getPriority())) {
                //同一级别的全部取出
                startServiceGroup(taskList);
                //开始指定下一级的启动组，此时上一级的已经全部启动完成，清空组
                taskList.clear();
            }
        }
        //最后一组的启动
        startServiceGroup(taskList);
    }

    private void stopServiceSync(List<ServiceSetting> settingList) {
        PriorityQueue<ServiceSetting> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(ServiceSetting::getPriority));
        priorityQueue.addAll(settingList);

        ArrayList<ServiceSetting> taskList = new ArrayList<>();
        ServiceSetting setting;
        while (null != (setting = priorityQueue.poll())) {
            taskList.add(setting);
            ServiceSetting next = priorityQueue.peek();
            if (null != next && !next.getPriority().equals(setting.getPriority())) {
                //同一级别的全部取出
                stopServiceGroup(taskList);
                //开始指定下一级的启动组，此时上一级的已经全部启动完成，清空组
                taskList.clear();
            }
        }
        //最后一组的启动
        stopServiceGroup(taskList);
    }

    private void startServiceGroup(List<ServiceSetting> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(setting ->
                executorService.execute(() -> {
                    try {
                        this.startSingleService(setting);
                    } finally {
                        countDownLatch.countDown();
                    }
                }));

        try {
            //等待全部启动完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private void stopServiceGroup(List<ServiceSetting> s) {
        if (CollectionUtils.isEmpty(s)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(s.size());
        s.forEach(server ->
                executorService.execute(() -> {
                    try {
                        this.stopSingleService(server);
                    } finally {
                        countDownLatch.countDown();
                    }
                }));

        try {
            //等待全部终止完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean needProxy(String host) {
        String self = ClusterClientManager.getInstance().getSelfHost();
        if (StringUtils.isNotEmpty(host) && StringUtils.isEmpty(self)) {
            // 非集群
            throw new JarbootException("当前为单机运行，调用集群接口失败！");
        }
        return !Objects.equals(self, host);
    }

    private void forEachOnlineClient(ClientCallback callback) {
        ClusterClientManager.getInstance().getHosts().forEach((k, v) -> {
            if (!ClusterServerState.ONLINE.equals(v)) {
                logger.warn("集群{}不在线！", k);
                return;
            }
            if (needProxy(k)) {
                ClusterClient client = ClusterClientManager.getInstance().getClient(k);
                if (null == client) {
                    logger.error("集群{}未初始化！", k);
                    return;
                }
                callback.invoke(client);
            }
        });
    }

    private interface ClientCallback {
        /**
         * 实现方法
         * @param client 客户端
         */
        void invoke(ClusterClient client);
    }
}
