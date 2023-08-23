package io.github.majianzheng.jarboot.core.session;

import io.github.majianzheng.jarboot.api.cmd.session.CommandSession;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.core.advisor.AdviceListener;
import io.github.majianzheng.jarboot.core.cmd.model.ResultModel;

import java.lang.instrument.ClassFileTransformer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author majianzheng
 */
public abstract class AbstractCommandSession implements CommandSession {
    protected boolean running = false;
    protected volatile String jobId = StringUtils.EMPTY;
    protected int row;
    protected int col;

    /**
     * 每执行一次命令生成一个唯一id
     * @return job id
     */
    @Override
    public String getJobId() {
        return jobId;
    }

    /**
     * 是否运行中
     * @return 是否在允许
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 开始执行
     */
    public abstract void setRunning();

    /**
     * 返回执行结果
     * @param resultModel 执行结果
     */
    public abstract void appendResult(ResultModel resultModel);

    /**
     * 注册监视器
     * @param adviceListener 监视器
     * @param transformer transformer
     */
    public abstract void register(AdviceListener adviceListener, ClassFileTransformer transformer);

    /**
     * 次数
     * @return 次数
     */
    public abstract AtomicInteger times();

    @Override
    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }
}
