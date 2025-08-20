package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.cluster.ClusterClient;
import io.github.majianzheng.jarboot.cluster.ClusterClientManager;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.service.UpgradeService;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import io.github.majianzheng.jarboot.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
    public ResponseSimple upgradeByPackage(
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            // 上传服务文件
            upgradeService.upgrade(filename, is, this::handleUpload);
        }
        return HttpResponseUtils.success();
    }

    /**
     * 从url下载安装包升级
     * @param url 下载的url
     * @return 执行结果
     */
    @PostMapping("url")
    public ResponseSimple upgradeByUrl(
            @RequestParam(value = "url", required = false) String url) {
        if (ClusterClientManager.getInstance().isEnabled()) {
            List<ClusterClient> clients = getAllNeedUpgradeClient();
            upgradeClusterClient(clients, url);
        }
        upgradeService.upgrade(url);
        return HttpResponseUtils.success();
    }

    private void handleUpload(File instFile) {
        if (!ClusterClientManager.getInstance().isEnabled()) {
            return;
        }
        List<ClusterClient> clients = getAllNeedUpgradeClient();
        if (CollectionUtils.isEmpty(clients)) {
            return;
        }

        // 将文件内容读取到内存中
        byte[] fileContent;
        try {
            fileContent = FileUtils.readFileToByteArray(instFile);
        } catch (IOException e) {
            throw new JarbootException("读取升级文件失败", e);
        }

        // 使用固定大小的线程池，避免创建过多线程
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(clients.size(), 3),
                JarbootThreadFactory.createThreadFactory("upgrade-client-")
        );

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> failedNodes = Collections.synchronizedList(new ArrayList<>());

        for (ClusterClient client : clients) {
            executor.submit(() -> {
                try (InputStream is = new ByteArrayInputStream(fileContent)) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    upgradeService.updateProgress(String.format("正在升级集群%s节点...", client.getHost()), CommonConst.STEP_FILE_INIT);
                    log.info("集群{}升级中...", client.getHost());
                    client.upgradeByPackage(is, instFile.getName());
                } catch (Exception e) {
                    log.error("集群{}升级失败！", client.getHost(), e);
                    upgradeService.updateProgress("集群" + client.getHost() + "升级失败！", -1);
                    failedNodes.add(client.getHost());
                }
            });
        }
        executor.shutdown();

        try {
            log.info("等待集群节点升级任务完成...");
            boolean await = executor.awaitTermination(5, TimeUnit.MINUTES);
            if (!await) {
                log.error("集群升级任务超时！");
                throw new JarbootException("集群升级超时！");
            }

            if (!failedNodes.isEmpty()) {
                throw new JarbootException("以下节点升级失败: " + String.join(", ", failedNodes));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JarbootException("升级过程被中断");
        }
    }

    private void upgradeClusterClient(List<ClusterClient> clients, String url) {
        if (CollectionUtils.isEmpty(clients)) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(clients.size());
        ThreadFactory threadFactory = JarbootThreadFactory.createThreadFactory("upgrade-client-");
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (ClusterClient client : clients) {
            threadFactory.newThread(() -> {
                try {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    upgradeService.updateProgress(String.format("正在升级集群%s节点...", client.getHost()), CommonConst.STEP_FILE_INIT);
                        log.info("集群{}根据url{}升级中...", client.getHost(), url);
                        client.upgradeByUrl(url);
                } catch (Exception e) {
                    log.error("集群{}升级失败！", client.getHost(), e);
                    upgradeService.updateProgress("集群" + client.getHost() + "升级失败！", -1);
                    throw new JarbootException("集群" + client.getHost() + "升级失败！");
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
        try {
            log.info("等待集群节点升级...");
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
                    String msg = "集群" + k + "不符合升级条件，需要单独升级，请检查：1.是否有服务在运行，2.是否在docker中！";
                    log.warn(msg);
                    upgradeService.updateProgress(msg, CommonConst.STEP_FILE_INIT);
                    MessageUtils.warn(msg);
                }
            }
        }
        return clients;
    }
}
