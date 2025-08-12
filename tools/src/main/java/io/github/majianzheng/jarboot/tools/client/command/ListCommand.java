package io.github.majianzheng.jarboot.tools.client.command;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.pojo.ServiceInstance;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * list command
 * @author majianzheng
 */
@Name("list")
@Summary("Display service info.")
@Description("Example:\n" +
        "  list\n")
public class ListCommand extends AbstractClientCommand {
    @Override
    public void run() {
        echoServices();
    }

    private void echoServices() {
        List<ServiceInstance> list = this.client.getServiceGroup();
        List<String> header = Arrays.asList("SID", "NAME", "GROUP", "STATUS", "HOST");
        List<List<String>> rows = new ArrayList<>();
        wrapperList(list, rows);
        String out = RenderUtil.renderTable(header, rows, terminal.getWidth(), null);
        println(out);
    }

    private void wrapperList(List<ServiceInstance> list, List<List<String>> rows) {
        list.forEach(service -> {
            if (CommonConst.NODE_ROOT == service.getNodeType() || CommonConst.NODE_GROUP == service.getNodeType()) {
                wrapperList(service.getChildren(), rows);
            } else {
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
            }
        });
    }

    @Override
    public void cancel() { // default implementation ignored

    }
}
