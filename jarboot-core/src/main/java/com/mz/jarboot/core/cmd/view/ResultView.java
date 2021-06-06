package com.mz.jarboot.core.cmd.view;

import com.mz.jarboot.core.cmd.model.ResultModel;

/**
 * Command execute result view interface
 * @author jianzhengma
 * @param <T>
 */
public interface ResultView<T extends ResultModel> {
    /**
     * MVC. convert data to html view
     * @param model Model data
     * @return to html page
     */
    String render(T model);
}
