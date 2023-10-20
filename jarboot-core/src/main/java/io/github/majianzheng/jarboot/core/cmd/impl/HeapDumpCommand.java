package io.github.majianzheng.jarboot.core.cmd.impl;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Option;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.core.cmd.AbstractCommand;
import io.github.majianzheng.jarboot.core.cmd.model.HeapDumpModel;
import io.github.majianzheng.jarboot.core.constant.CoreConstant;
import io.github.majianzheng.jarboot.core.utils.LogUtils;
import com.sun.management.HotSpotDiagnosticMXBean; //NOSONAR
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HeapDump command
 * 
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
@Name("heapdump")
@Summary("Heap dump")
@Description("\nExamples:\n" + "  heapdump\n" + "  heapdump --live\n"
                + CoreConstant.WIKI + CoreConstant.WIKI_HOME + "heapdump")
public class HeapDumpCommand extends AbstractCommand {
    private static final Logger logger = LogUtils.getLogger();

    private boolean live;

    @Option(shortName = "l", longName = "live", flag = true)
    @Description("Dump only live objects; if not specified, all objects in the heap are dumped.")
    public void setLive(boolean live) {
        this.live = live;
    }

    private void cleanOldDump(File dir) {
        //文件夹中最多存放5个文件，超了则删除最老的
        File[] files = dir.listFiles();
        if (null == files || files.length < 5) {
            return;
        }
        List<File> sorted = Arrays.stream(files).sorted((a, b) -> (int) (a.lastModified() - b.lastModified()))
                .collect(Collectors.toList());
        Iterator<File> iter = sorted.iterator();
        while (iter.hasNext() && sorted.size() > 4) {
            File file = iter.next();
            iter.remove();
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    @SuppressWarnings("squid:S1181")
    public void run() {
        String outPath = LogUtils.getLogDir() + File.separator + "dump";
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
            String dumpFile = "heapdump" + date + (live ? "-live.hprof" : ".hprof");
            String destFile = outPath + File.separator + dumpFile;
            File file = new File(destFile);
            if (file.exists()) {
                FileUtils.forceDelete(file);
            }
            session.console("Dumping heap to " + dumpFile + " ...");
            File dir = new File(outPath);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.warn("创建dump目录失败！");
                session.end(false, "执行失败，无法创建dump目录");
                return;
            } else {
                cleanOldDump(dir);
            }
            run(destFile, live);
            session.appendResult(new HeapDumpModel(file, live));
            session.end(true, "Heap dump file created");
        } catch (Throwable t) {
            String errorMsg = "heap dump error: " + t.getMessage();
            logger.error(errorMsg, t);
            session.end(false, errorMsg);
        }
    }

    private static void run(String file, boolean live) throws IOException {
        HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean = ManagementFactory
                        .getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        hotSpotDiagnosticMXBean.dumpHeap(file, live);
    }

}
