package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.JvmItem;
import com.mz.jarboot.core.cmd.model.JvmModel;
import com.mz.jarboot.core.utils.StringUtils;

import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author majianzheng
 */
public class JvmView implements ResultView<JvmModel> {
    @Override
    public String render(JvmModel model) {
        StringBuilder sb = new StringBuilder();
        //RuntimeInfo
        renderJvmItemList(sb, model.getRuntimeInfo(), "RUNTIME");

        //CLASS-LOADING
        renderJvmItemList(sb, model.getClassLoadingInfo(), "CLASS-LOADING");

        //COMPILATION
        renderJvmItemList(sb, model.getCompilation(), "COMPILATION");

        //GARBAGE-COLLECTORS
        renderGarbageCollectors(sb, model.getGarbageCollectorsInfo());

        //MEMORY-MANAGERS
        renderJvmItemList(sb, model.getMemoryMgrInfo(), "MEMORY-MANAGERS");

        //MEMORY
        renderMemory(sb, model);

        //OPERATING-SYSTEM
        renderJvmItemList(sb, model.getOperatingSystemInfo(), "OPERATING-SYSTEM");

        //THREAD
        renderJvmItemList(sb, model.getThreadInfo(), "THREAD");

        //FILE-DESCRIPTOR
        renderJvmItemList(sb, model.getFileDescInfo(), "FILE-DESCRIPTOR");
        return sb.toString();
    }

    private void renderJvmItemList(StringBuilder sb, List<JvmItem> list, String title) {
        List<List<String>> rows = new ArrayList<>();
        list.forEach(item -> {
            List<String> row = new ArrayList<>();
            row.add(item.getName());
            row.add(item.getValue().toString());
            rows.add(row);
        });
        sb.append(ViewRenderUtil.renderTable(null , rows, title, 1));
    }

    private void renderGarbageCollectors(StringBuilder sb, List<JvmModel.GarbageCollectorItem> list) {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        headers.add(StringUtils.EMPTY);
        headers.add("collectionCount");
        headers.add("collectionTime (ms)");
        list.forEach(item -> {
            List<String> row = new ArrayList<>();
            row.add(item.getName());
            row.add(String.valueOf(item.getCollectionCount()));
            row.add(String.valueOf(item.getCollectionTime()));
            rows.add(row);
        });
        sb.append(ViewRenderUtil.renderTable(headers , rows, "GARBAGE-COLLECTORS", 1));
    }

    private void renderMemory(StringBuilder sb, JvmModel model) {
        List<JvmItem> info = model.getMemoryInfo();
        List<List<String>> rows = new ArrayList<>();
        info.forEach(item -> {
            List<String> row = new ArrayList<>();
            row.add(item.getName());
            MemoryUsage usage = (MemoryUsage)item.getValue();
            row.add(String.format("init : %d<br>used : %d<br>committed : %d<br>max : %d",
                    usage.getInit(), usage.getUsed(), usage.getCommitted(), usage.getMax()));
            rows.add(row);
        });

        if (model.getPendingFinalizationCount() > 0) {
            List<String> tail = new ArrayList<>();
            tail.add("PENDING-FINALIZE-COUNT");
            tail.add(String.valueOf(model.getPendingFinalizationCount()));
            rows.add(tail);
        }
        sb.append(ViewRenderUtil.renderTable(null , rows, "MEMORY", 1));
    }
}
