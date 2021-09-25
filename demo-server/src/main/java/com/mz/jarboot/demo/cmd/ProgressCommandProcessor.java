package com.mz.jarboot.demo.cmd;

import com.mz.jarboot.api.cmd.annotation.Description;
import com.mz.jarboot.api.cmd.annotation.Name;
import com.mz.jarboot.api.cmd.annotation.Summary;
import com.mz.jarboot.api.cmd.session.CommandSession;
import com.mz.jarboot.api.cmd.spi.CommandProcessor;

/**
 * progress 测试在Console终端显示动态进度条的命令
 * @author majianzheng
 */
@Name("progress")
@Summary("测试动态进度条在Console终端显示的命令，使用CommandSession的backspaceLine方法编码实现")
@Description("Example:\n progress")
public class ProgressCommandProcessor implements CommandProcessor {
    private volatile boolean stopped = false;
    @Override
    public String process(CommandSession session, String[] args) {
        int i = 0;
        final int maxValue = 100;
        session.console(this.genProgressHtml(i));
        for (; i <= maxValue; ++i) {
            session.backspaceLine(this.genProgressHtml(i));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopped = true;
            }
            if (stopped) {
                break;
            }
        }
        return "";
    }

    @Override
    public void onCancel() {
        stopped = true;
    }

    private String genProgressHtml(int i) {
        String percent = i + "%";
        String text = String.format("<span style=\"font-size:18px;\">%s</span>", percent);
        String style = String.format(
                "style=\"text-align:center;height:30px;width:%s;background:green;margin:0 8px 0 8px\"", percent);
        return String.format("<div><div %s>%s</div></div>", style, text);
    }
}
