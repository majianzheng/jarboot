package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.UpgradeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * jarboot服务运行时信息
 * @author mazheng
 */
@Slf4j
@RequestMapping(value = "/api/jarboot/upgrade")
@RestController
public class UpgradeController {
    @Resource
    private UpgradeService upgradeService;
    /**
     * 上传安装包升级
     * @param file 文件
     * @return 执行结果
     */
    @PostMapping("upload")
    public ResponseSimple upload(
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            if (ClusterClientManager.getInstance().isEnabled()) {
                List<ClusterClient> clients = getAllNeedUpgradeClient();
                upgradeClusterClient(clients, false, null, filename, is);
            }
            // 上传服务文件
            upgradeService.upgrade(filename, is);
        }
        return HttpResponseUtils.success();
    }

    /**
     * 从url下载安装包升级
     * @param url 下载的url
     * @return 执行结果
     */
    @PostMapping("url")
    public ResponseSimple upload(
            @RequestParam(value = "url", required = false) String url) {
        if (ClusterClientManager.getInstance().isEnabled()) {
            List<ClusterClient> clients = getAllNeedUpgradeClient();
            upgradeClusterClient(clients, true, url, null, null);
        }
        upgradeService.upgrade(url);
        return HttpResponseUtils.success();
    }

    private void upgradeClusterClient(List<ClusterClient> clients, boolean isUrl, String url, String filename, InputStream is) {
        if (CollectionUtils.isEmpty(clients)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(clients.size());
        ThreadFactory threadFactory = JarbootThreadFactory.createThreadFactory("upgrade-client-");
        for (ClusterClient client : clients) {
            threadFactory.newThread(() -> {
                try {
                    if (isUrl) {
                        client.upgradeByUrl(url);
                    } else {
                        client.upgradeByPackage(is, filename);
                    }
                } catch (Exception e) {
                    log.error("集群{}升级失败！", client.getHost());
                    upgradeService.updateProgress("集群" + client.getHost() + "升级失败！", -1);
                    throw new JarbootException("集群" + client.getHost() + "升级失败！");
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
        try {
            boolean await = countDownLatch.await(3, TimeUnit.MINUTES);
            if (!await) {
                log.error("集群升级超时！");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<ClusterClient> getAllNeedUpgradeClient() {
        List<ClusterClient> clients = new ArrayList<>();
        Map<String, ClusterClient> clientMap = ClusterClientManager.getInstance().getHosts();
        for (Map.Entry<String, ClusterClient> entry : clientMap.entrySet()) {
            String k = entry.getKey();
            if (CommonUtils.needProxy(k)) {
                ClusterClient v = entry.getValue();
                if (!v.isOnline()) {
                    log.warn("集群{}不在线！", k);
                    upgradeService.updateProgress("集群" + k + "不在线！", -1);
                    throw new JarbootException("集群" + k + "不在线！");
                }
                if (v.upgradeCheck()) {
                    clients.add(v);
                } else {
                    log.warn("集群{}不符合升级条件，请检查：1.是否有服务在运行，2.是否在docker中！", k);
                    upgradeService.updateProgress("集群" + k + "不符合升级条件，请检查：1.是否有服务在运行，2.是否在docker中！", 0);
                }
            }
        }
        return clients;
    }
}
