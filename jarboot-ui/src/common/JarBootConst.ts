import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";

class JarBootConst {
    public static MSG_TYPE_START = "START";
    public static MSG_TYPE_STOP = "STOP";
    public static MSG_TYPE_RESTART = "RESTART";
    public static MSG_TYPE_STARTED = "STARTED";
    public static MSG_TYPE_STOPPED = "STOPPED";
    public static MSG_TYPE_START_ERROR = "START_ERROR";
    public static MSG_TYPE_STOP_ERROR = "STOP_ERROR";
    public static MSG_TYPE_OUT = "OUT";
    private static MSG_TYPE_CMD_FINISH = "CMD_ACK";

    //进程状态
    public static STATUS_STARTED = '正在运行';
    public static STATUS_STOPPED = '已停止';
    public static STATUS_STARTING = '正在启动';
    public static STATUS_STOPPING = '正在停止';
    public static STATUS_START_ERROR = '启动失败';

    public static NOTICE_INFO = "NOTICE_INFO";
    public static NOTICE_WARN = "NOTICE_WARN";
    public static NOTICE_ERROR = "NOTICE_ERROR";
}
interface MessageBody {
    server: string;
    serverType: string;
    msgType: string;
    text: string;
}

const requestFinishCallback = (resp: any) => {
    if (resp.resultCode === 0) {
        CommonNotice.info('请求成功');
    } else {
        CommonNotice.error(ErrorUtil.formatErrResp(resp));
    }
};

export {JarBootConst, MessageBody, requestFinishCallback};
