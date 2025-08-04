package io.github.majianzheng.jarboot.tools.client.command;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.cmd.annotation.*;
import io.github.majianzheng.jarboot.common.AnsiLog;
import io.github.majianzheng.jarboot.common.JarbootThreadFactory;
import io.github.majianzheng.jarboot.common.pojo.UploadFileParam;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.tools.client.InnerConsole;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.websocket.WsWebSocketContainer;

import javax.websocket.*;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Deploy command.
 * @author majianzheng
 */
@Name("deploy")
@Summary("Display deploy command. ig. info")
@Description("Usage:\n" +
        "-s [service name]\n" +
        "-h [cluster host only cluster mode]\n" +
        "-a [flag, auto stop service before deploy and start after deploy]\n" +
        "-d [remote service directory to upload, upload to service root directory if not set]\n" +
        "Example:\n" +
        "  deploy -s demo-service ./demo-service.jar\n" +
        "  deploy -h 127.0.0.1:9899 -s demo-service -a ./demo-service.jar\n" +
        "  deploy -s demo-server -d file_dir ./file_dir  (file_dir list file will upload to file_dir in service dir)\n" +
        "  deploy -s demo-server ./file_dir (file_dir list file will upload to service root dir)\n")
@ClientEndpoint
public class DeployCommand extends AbstractClientCommand {
    private static final int SEND_COUNT_ONCE = 2000;
    private String host;
    private String serviceName;
    private String path;
    private Session session;
    private File uploadFile;
    private long totalSize = 0;
    private volatile long uploadSize = 0;
    private volatile boolean cancel = false;
    private ProgressBar bar;
    private CountDownLatch latch;
    private boolean autoRestart;
    private String userDir;
    private String remoteDir;
    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock connLock = new ReentrantLock();

    @Option(shortName = "h", longName = "host")
    @Description("Cluster host")
    public void setHost(String host) {
        this.host = host;
    }
    @Option(shortName = "s", longName = "service")
    @Description("service name")
    public void setServiceNames(String serviceName) {
        this.serviceName = serviceName;
    }
    @Option(shortName = "a", longName = "auto", flag = true)
    @Description("service name")
    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }
    @Option(shortName = "d", longName = "dir")
    @Description("remote service dir")
    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    @Argument(argName = "path", index = 0)
    @Description("file or directory.")
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void run() {
        File file;
        if (Paths.get(path).isAbsolute()) {
            file = FileUtils.getFile(path);
        } else {
            file = FileUtils.getFile(InnerConsole.getInstance().getWordDir(), path);
        }
        if (!file.exists()) {
            println(file.getAbsolutePath() + " is not exists.");
            return;
        }
        final String flag = "://";
        if (!loginHost.contains(flag)) {
            loginHost = "ws://" + loginHost;
        } else {
            loginHost = loginHost.replace("http://", "ws://").replace("https://", "wss://");
        }
        userDir = client.getUserDir();

        WsWebSocketContainer container = new WsWebSocketContainer();
        Thread.currentThread().setContextClassLoader(DeployCommand.class.getClassLoader());
        try {
            if (autoRestart) {
                ServiceCommand serviceCommand = createServiceCommand("stop");
                serviceCommand.run();
            }
            if (file.isFile()) {
                uploadFile = file;
                String name = StringUtils.isEmpty(remoteDir) ? file.getName() : String.format("%s/%s", remoteDir, file.getName());
                connectTo(container, name);
            } else {
                uploadDir(container, file, remoteDir);
            }
            if (autoRestart) {
                ServiceCommand serviceCommand = createServiceCommand("start");
                serviceCommand.run();
            }
        } catch (Exception e) {
            AnsiLog.error(e.getMessage(), e);
        } finally {
            container.destroy();
        }
    }

    private ServiceCommand createServiceCommand(String action) {
        ServiceCommand cmd = new ServiceCommand();
        cmd.proxy = proxy;
        cmd.client = client;
        cmd.runtimeInfo = runtimeInfo;
        cmd.clusterMode = clusterMode;
        cmd.name = "service";
        cmd.setAction(action);
        if (clusterMode && StringUtils.isNotEmpty(host)) {
            cmd.setServiceNames(String.format("%s@%s", serviceName, host));
        } else {
            cmd.setServiceNames(serviceName);
        }
        cmd.setLineReader(lineReader);
        cmd.loginHost = loginHost;
        return cmd;
    }

    private void uploadDir(WsWebSocketContainer container, File dir, String relPath) {
        File[] files = dir.listFiles();
        if (null == files || cancel) {
            return;
        }
        for (File file : files) {
            String fullPath = StringUtils.isEmpty(relPath) ? file.getName() : String.format("%s/%s", relPath, file.getName());
            if (file.isDirectory()) {
                uploadDir(container, file, fullPath);
            } else {
                uploadFile = file;
                connectTo(container, fullPath);
            }
        }
    }

    private void connectTo(WsWebSocketContainer container, String relPath) {
        if (cancel) {
            return;
        }
        try {
            connLock.lock();
            UploadFileParam param = new UploadFileParam();
            param.setFilename(uploadFile.getName());
            param.setDstPath(String.format("%s/%s/%s", userDir, serviceName, relPath));
            param.setRelativePath(relPath);
            param.setUploadMode("workspace");
            param.setTotalSize(uploadFile.length());
            param.setSendCountOnce(SEND_COUNT_ONCE);
            if (clusterMode) {
                param.setClusterHost(host);
            }
            String paramStr = Base64.getUrlEncoder().encodeToString(JsonUtils.toJsonBytes(param));
            String url = loginHost + "/jarboot/upload/ws?params=" + paramStr;
            url += "&accessToken=" + proxy.getToken();
            if (clusterMode) {
                url += "&Access-Cluster-Host=" + URLEncoder.encode(runtimeInfo.getHost(), StandardCharsets.UTF_8.name());
            }
            latch = new CountDownLatch(1);
            session = container.connectToServer(this, URI.create(url));
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        } finally {
            latch = null;
            connLock.unlock();
        }
    }

    private synchronized void sendFile() {
        lock.lock();
        try (InputStream os = FileUtils.openInputStream(uploadFile)) {
            byte[] data = new byte[4000];
            int sendCount = 0;
            if (uploadSize > 0) {
                os.skip(uploadSize);
            }

            while (true) {
                int len = os.read(data);
                if (len <= 0 || cancel) {
                    break;
                }
                if (sendCount % SEND_COUNT_ONCE == 0) {
                    this.wait(5000);
                }
                session.getBasicRemote().sendBinary(java.nio.ByteBuffer.wrap(data, 0, len));
                if (sendCount % 10 == 0) {
                    Thread.yield();
                }
                sendCount++;
                uploadSize += len;
                updateLocal();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AnsiLog.error(e);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void cancel() {
        cancel = true;
        finishOne();
    }

    @OnOpen
    public void onOpen() {
        // ignored
    }

    @OnMessage
    public void onMessage(String message) {
        JsonNode json = JsonUtils.readAsJsonNode(message);
        if (null == json) {
            return;
        }
        uploadSize = json.get("uploadSize").asLong(0);
        if (null == bar) {
            startUpload();
        } else {
            updateProgress();
        }
    }

    private synchronized void startUpload() {
        totalSize = uploadFile.length();
        String unitName = "B";
        long unitSize = 1;
        final long tb = 1024L * 1024 * 1024 * 1024;
        final long gb = 1024L * 1024 * 1024;
        final long mb = 1024L * 1024;
        final long kb = 1024;
        final long c = 8;
        if (totalSize > tb * c) {
            unitName = "TB";
            unitSize = tb;
        } else if (totalSize > gb * c) {
            unitName = "GB";
            unitSize = gb;
        } else if (totalSize > mb * c) {
            unitName = "MB";
            unitSize = mb;
        } else if (totalSize > kb * c) {
            unitName = "KB";
            unitSize = kb;
        }

        AnsiLog.println("Uploading {}...", uploadFile.getName());
        bar = new ProgressBarBuilder()
                .setInitialMax(totalSize)
                .setUnit(unitName, unitSize)
                .setSpeedUnit(ChronoUnit.SECONDS)
                .showSpeed().showSpeed().build();
        JarbootThreadFactory.createThreadFactory("deploy-").newThread(this::sendFile).start();
    }

    @OnClose
    public void onClosed(Session session) {
        updateProgress();
    }

    @OnError
    public void onFailure(Throwable t) {
        updateProgress();
        AnsiLog.error(t);
    }

    private synchronized void updateProgress() {
        this.notifyAll();
        try {
            ProgressBar temp = bar;
            if (null != temp) {
                temp.stepTo(uploadSize);
                if (uploadSize >= totalSize) {
                    temp.close();
                    bar = null;
                    finishOne();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void updateLocal() {
        try {
            ProgressBar temp = bar;
            if (null != temp) {
                temp.stepTo(uploadSize);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void finishOne() {
        CountDownLatch temp = latch;
        if (null != temp) {
            temp.countDown();
            latch = null;
        }
    }
}
