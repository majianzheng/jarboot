package com.mz.jarboot.core.cmd.impl;

import com.mz.jarboot.core.session.CommandSession;

/**
 * @author majianzheng
 * 以下代码基于开源项目Arthas适配修改
 */
public class PathTraceAdviceListener extends AbstractTraceAdviceListener {

    public PathTraceAdviceListener(TraceCommandImpl command, CommandSession process) {
        super(command, process);
    }
}
