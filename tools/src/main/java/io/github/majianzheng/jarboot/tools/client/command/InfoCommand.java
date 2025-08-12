package io.github.majianzheng.jarboot.tools.client.command;

import io.github.majianzheng.jarboot.api.cmd.annotation.Description;
import io.github.majianzheng.jarboot.api.cmd.annotation.Name;
import io.github.majianzheng.jarboot.api.cmd.annotation.Summary;
import io.github.majianzheng.jarboot.api.pojo.HostInfo;
import io.github.majianzheng.jarboot.api.pojo.ServerRuntimeInfo;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.text.ui.TableElement;
import io.github.majianzheng.jarboot.text.util.RenderUtil;

import java.util.List;

/**
 * @author majianzheng
 */
@Name("info")
@Summary("Display info command. ig. info")
@Description("Example:\n" +
        "  info\n")
public class InfoCommand extends AbstractClientCommand {
    @Override
    public void run() {
        ServerRuntimeInfo runtimeInfo = proxy.getRuntimeInfo();
        TableElement table = new TableElement();
        table.rightCellPadding(1).rightCellPadding(1);
        table.row(true, "NAME", "DESCRIPTION");
        table.row("Jarboot Version", runtimeInfo.getVersion());
        table.row("Jarboot Host", withDefault(runtimeInfo.getHost()));
        table.row("Cluster Mode", StringUtils.isEmpty(runtimeInfo.getHost()) ? "No" : "Yes");
        table.row("Machine Code", runtimeInfo.getMachineCode());

        println(RenderUtil.render(table, terminal.getWidth()));
        if (StringUtils.isNotEmpty(runtimeInfo.getHost())) {
            println("Cluster hosts:");
            table = new TableElement();
            table.rightCellPadding(1).rightCellPadding(1);
            table.row(true, "NAME", "HOST", "STATE");
            List<HostInfo> hostInfos = client.getOnlineClusterHosts();
            for (HostInfo hostInfo : hostInfos) {
                table.row(withDefault(hostInfo.getName()), hostInfo.getHost(), hostInfo.getState().name());
            }
            println(RenderUtil.render(table, terminal.getWidth()));
        }
    }

    @Override
    public void cancel() {// default implementation ignored
    }
}
