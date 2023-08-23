package io.github.majianzheng.jarboot.core.cmd.view;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.core.cmd.model.ResultModel;

/**
 * Command execute result view interface
 * @author majianzheng
 * @param <T>
 */
public interface ResultView<T extends ResultModel> {
    /**
     * MVC. convert data to html, text or json view
     * @param session Session
     * @param model Model data
     * @return to html page
     */
    String render(CommandSession session, T model);

    /**
     * 是否json，当是json时，由前端代码渲染
     * @return 为json字符串时，为true
     */
    default boolean isJson() {
        return false;
    }
}
