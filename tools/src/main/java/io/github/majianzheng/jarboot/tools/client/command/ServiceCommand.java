package io.github.majianzheng.jarboot.tools.client.command;

import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.constant.TaskLifecycle;
import io.github.majianzheng.jarboot.api.event.JarbootEvent;
import io.github.majianzheng.jarboot.api.event.Subscriber;
import io.github.majianzheng.jarboot.api.event.TaskLifecycleEvent;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * list command
 * @author majianzheng
 */
@Name("service")
@Summary("service operation. ig. service start demo-service")
@Description("Usage:\n" +
        "-a [flag, all service]\n" +
        "arg1 [action start,stop,status or restart]\n" +
        "arg2 [service name, use ',' split if multi service, cluster host can use '@' set host]\n" +
        "Example:\n" +
        "  service start demo-server@192.168.1.100:9899,test-server@192.168.1.101:9899,other\n" +
        "  service stop demo-service\n" +
        "  service status demo-service\n" +
        "  service -a stop\n" +
        "  service -a start\n" +
        "  service restart demo-service\n")
public class ServiceCommand extends AbstractClientCommand implements Subscriber<TaskLifecycleEvent> {
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_RESTART = "restart";
    private static final String ACTION_STATUS = "status";

    private String action;
    private List<String> serviceNames;
    private Boolean all;
    private CountDownLatch latch;

    @Argument(argName = "action", index = 0)
    @Description("action, input start stop restart or get.")
    public void setAction(String action) {
        this.action = action;
    }

    @Argument(argName = "name", index = 1, required = false)
    @Description("service names")
    public void setServiceNames(String serviceNames) {
        this.serviceNames = Arrays.asList(serviceNames.split(","));
    }
    @Option(longName = "all", shortName = "a", flag = true)
    @Description("all service")
    public void setAll(Boolean all) {
        this.all = all;
    }
    @Override
    public void run() {
        List<ServiceInstance> service = getServiceList();
        if (null == service || service.isEmpty()) {
            printHelp();
            return;
        }
        client.registerSubscriber(this);
        final String tips = "Service " + String.join(",", serviceNames) + " " + action;
        try {
            latch = new CountDownLatch(service.size());
            switch (action) {
                case ACTION_START:
                    client.startService(service);
                    break;
                case ACTION_STOP:
                    client.stopService(service);
                    break;
                case ACTION_RESTART:
                    client.restartService(service);
                    break;
                case ACTION_STATUS:
                    echoServices();
                    break;
                default:
                    printHelp();
                    return;
            }
            final long timeout = 30;
            if (latch.await(timeout, java.util.concurrent.TimeUnit.SECONDS)) {
                println(tips + " success.");
            } else {
                println(tips + " timeout.");
            }
        } catch (InterruptedException e) {
            println(tips + " interrupted.");
            Thread.currentThread().interrupt();
        } finally {
            client.deregisterSubscriber(this);
        }
    }

    @Override
    public void cancel() {  // default implementation ignored
    }

    @Override
    public void onEvent(TaskLifecycleEvent event) {
        synchronized (this) {
            switch (action) {
                case ACTION_START:
                case ACTION_RESTART:
                    if (event.getLifecycle() == TaskLifecycle.FINISHED ||
                            event.getLifecycle() == TaskLifecycle.AFTER_STARTED ||
                            event.getLifecycle() == TaskLifecycle.START_FAILED ||
                            event.getLifecycle() == TaskLifecycle.SCHEDULING) {
                        latch.countDown();
                    }
                    break;
                case ACTION_STOP:
                    if (event.getLifecycle() == TaskLifecycle.AFTER_STOPPED ||
                            event.getLifecycle() == TaskLifecycle.STOP_FAILED) {
                        latch.countDown();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public Class<? extends JarbootEvent> subscribeType() {
        return TaskLifecycleEvent.class;
    }

    private void echoServices() {
        List<ServiceInstance> list = this.client.getServiceGroup();
        List<String> header = Arrays.asList("SID", "NAME", "GROUP", "STATUS", "HOST");
        List<List<String>> rows = new ArrayList<>();
        wrapperRows(list, rows);
        String out = RenderUtil.renderTable(header, rows, terminal.getWidth(), null);
        println(out);
    }

    private List<ServiceInstance> getServiceList() {
        if (Boolean.TRUE.equals(all)) {
            this.serviceNames = new ArrayList<>();
            List<ServiceInstance> list = this.client.getServiceGroup();
            List<ServiceInstance> services = new ArrayList<>();
            wrapperList(list, services);
            return services;
        }
        return serviceNames.stream().map(name -> {
            ServiceInstance instance = new ServiceInstance();
            if (clusterMode) {
                int index = name.indexOf("@");
                if (index > 0) {
                    instance.setHost(name.substring(index + 1).trim());
                    name = name.substring(0, index);
                }
            }
            instance.setName(name);
            return instance;
        }).collect(Collectors.toList());
    }

    private void wrapperList(List<ServiceInstance> list, List<ServiceInstance> rows) {
        list.forEach(service -> {
            if (CommonConst.NODE_ROOT == service.getNodeType() || CommonConst.NODE_GROUP == service.getNodeType()) {
                wrapperList(service.getChildren(), rows);
            } else {
                serviceNames.add(service.getName());
                rows.add(service);
            }
        });
    }

    private void wrapperRows(List<ServiceInstance> list, List<List<String>> rows) {
        list.forEach(service -> {
            if (CommonConst.NODE_ROOT == service.getNodeType() || CommonConst.NODE_GROUP == service.getNodeType()) {
                wrapperRows(service.getChildren(), rows);
            } else {
                if (matchService(service)) {
                    return;
                }
                String host = service.getHost();
                if (StringUtils.isNotEmpty(service.getHostName())) {
                    host = String.format("%s(%s)", service.getHostName(), service.getHost());
                }
                rows.add(Arrays.asList(
                        service.getSid(),
                        service.getName(),
                        withDefault(service.getGroup()),
                        withDefault(service.getStatus()),
                        withDefault(host)));
                latch.countDown();
            }
        });
    }

    private boolean matchService(ServiceInstance service) {
        for (String serviceName : serviceNames) {
            int index = serviceName.indexOf("@");
            if (clusterMode && index > 0) {
                String host = serviceName.substring(index + 1).trim();
                serviceName = serviceName.substring(0, index);
                if (host.equals(service.getHost()) && serviceName.equals(service.getName())) {
                    return false;
                }
            } else {
                if (serviceName.equals(service.getName())) {
                    return false;
                }
            }
        }
        return true;
    }
}
