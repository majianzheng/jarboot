package io.github.majianzheng.jarboot.cluster;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.*;
import io.github.majianzheng.jarboot.api.service.ServiceManager;
import io.github.majianzheng.jarboot.api.service.SettingService;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
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
    @Resource(name = "taskExecutorService")
    private ExecutorService executorService;

    public List<ServiceInstance> getServiceGroup() {
        List<ServiceInstance> groups = new ArrayList<>();
        // 获取集群其它服务器的组信息
        if (!ClusterClientManager.getInstance().isEnabled()) {
            ServiceInstance localGroup = serviceManager.getServiceGroup();
            groups.add(localGroup);
            return groups;
        }
        forEachOnlineClient((proxy, client) -> groups.add(getServiceGroup(proxy, client)));
        return groups;
    }
    private ServiceInstance getServiceGroup (boolean proxy, ClusterClient client) {
        if (proxy) {
            if (client.isOnline()) {
                try {
                    return client.getServiceGroup();
                } catch (Exception e) {
                    client.setState(ClusterServerState.OFFLINE);
                    logger.error(e.getMessage(), e);
                }
            }
            ServiceInstance group = new ServiceInstance();
            initDefaultNode(group, client);
            return group;
        }
        return serviceManager.getServiceGroup();
    }

    public List<JvmProcess> getJvmGroup() {
        List<JvmProcess> groups = new ArrayList<>();
        // 获取集群其它服务器的组信息
        if (!ClusterClientManager.getInstance().isEnabled()) {
            JvmProcess localGroup = serviceManager.getJvmGroup();
            groups.add(localGroup);
            return groups;
        }
        forEachOnlineClient((proxy, client) -> groups.add(getJvmGroup(proxy, client)));
        return groups;
    }

    private JvmProcess getJvmGroup (boolean proxy, ClusterClient client) {
        if (proxy) {
            if (client.isOnline()) {
                try {
                    return client.getJvmGroup();
                } catch (Exception e) {
                    client.setState(ClusterServerState.OFFLINE);
                    logger.error(e.getMessage(), e);
                }
            }
            JvmProcess group = new JvmProcess();
            initDefaultNode(group, client);
            return group;
        }
        return serviceManager.getJvmGroup();
    }

    public void startService(List<ServiceInstance> services) {
        List<ServiceSetting> settingList = services
                .stream()
                .map(this::getServiceSetting)
                .collect(Collectors.toList());
        TaskUtils.getTaskExecutor().execute(() -> startServiceSync(settingList));
    }

    public void stopService(List<ServiceInstance> services) {
        List<ServiceSetting> settingList = services
                .stream()
                .map(this::getServiceSetting)
                .collect(Collectors.toList());
        TaskUtils.getTaskExecutor().execute(() -> stopServiceSync(settingList));
    }

    public void restartService(List<ServiceInstance> services) {
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
        if (CommonUtils.needProxy(host)) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(host);
            client.attach(pid);
        } else {
            serviceManager.attach(pid);
        }
    }

    public void deleteService(ServiceInstance instance) {
        if (CommonUtils.needProxy(instance.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(instance.getHost());
            client.deleteService(instance.getName());
        } else {
            serviceManager.deleteService(instance.getName());
        }
    }

    /**
     * 启动单个服务
     * @param setting 服务配置
     */
    public void startSingleService(ServiceSetting setting) {
        if (CommonUtils.needProxy(setting.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(setting.getHost());
            final int maxWait = SettingUtils.getSystemSetting().getMaxStartTime() + 1500;
            String resp = client.requestSync(ClusterEventName.START_SERVICE, JsonUtils.toJsonString(setting), maxWait);
            ResponseSimple response = JsonUtils.readValue(resp, ResponseSimple.class);
            if (null == response || !response.getSuccess()) {
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
        if (CommonUtils.needProxy(setting.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(setting.getHost());
            final int maxWait = SettingUtils.getSystemSetting().getMaxExitTime() + 5000;
            String resp = client.requestSync(ClusterEventName.STOP_SERVICE, JsonUtils.toJsonString(setting), maxWait);
            ResponseSimple response = JsonUtils.readValue(resp, ResponseSimple.class);
            if (null == response || !response.getSuccess()) {
                logger.error(resp);
            }
        } else {
            serviceManager.stopSingleService(setting);
        }
    }

    public ServiceSetting getServiceSetting(ServiceInstance instance) {
        if (CommonUtils.needProxy(instance.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(instance.getHost());
            if (null == client) {
                throw new JarbootException("集群实例不存在，host:"  + instance.getHost());
            }
            return client.getServiceSetting(instance.getName());
        } else {
            return settingService.getServiceSetting(instance.getName());
        }
    }

    public void saveServiceSetting(ServiceSetting setting) {
        if (CommonUtils.needProxy(setting.getHost())) {
            ClusterClient client = ClusterClientManager.getInstance().getClient(setting.getHost());
            if (null == client) {
                throw new JarbootException("集群实例不存在，host:"  + setting.getHost());
            }
            client.saveServiceSetting(setting);
        } else {
            settingService.submitServiceSetting(setting);
        }
    }

    private void initDefaultNode(BaseInstanceNode node, ClusterClient client) {
        node.setNodeType(CommonConst.NODE_ROOT);
        node.setHost(client.getHost());
        node.setName(client.getName());
        node.setSid(String.format("%08x", Objects.hash(System.currentTimeMillis(), client.getHost())));
        node.setStatus(client.getState().name());
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

    private void forEachOnlineClient(ClientCallback callback) {
        ClusterClientManager.getInstance().getHosts().forEach((k, v) -> {
            if (!v.isOnline()) {
                logger.warn("集群{}不在线！", k);
            }
            callback.invoke(CommonUtils.needProxy(k), v);
        });
    }

    private interface ClientCallback {
        /**
         * 实现方法
         * @param proxy 是否需要转发
         * @param client 客户端
         */
        void invoke(boolean proxy, ClusterClient client);
    }
}
