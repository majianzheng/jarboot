import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";

class JarBootConst {
    public static DOCS_URL = "https://www.yuque.com/jarboot/usage/quick-start";
    public static MSG_TYPE_START = "START";
    public static MSG_TYPE_STOP = "STOP";
    public static MSG_TYPE_RESTART = "RESTART";
    public static MSG_TYPE_STARTED = "STARTED";
    public static MSG_TYPE_STOPPED = "STOPPED";
    public static MSG_TYPE_START_ERROR = "START_ERROR";
    public static MSG_TYPE_STOP_ERROR = "STOP_ERROR";
    public static MSG_TYPE_OUT = "OUT";
    public static MSG_TYPE_CMD_COMPLETE = "CMD_COMPLETE";

    public static PROTOCOL_SPLIT = '\r';

    //进程状态
    public static STATUS_STARTED = 'RUNNING';
    public static STATUS_STOPPED = 'STOPPED';
    public static STATUS_STARTING = 'STARTING';
    public static STATUS_STOPPING = 'STOPPING';
    public static STATUS_START_ERROR = '启动失败';

    public static NOTICE_INFO = 0;
    public static NOTICE_WARN = 1;
    public static NOTICE_ERROR = 2;

    public static PANEL_HEIGHT = (window.innerHeight - 150);

    public static ZH_CN = 'zh-CN';

    //token
    public static TOKEN_KEY = 'token';
    public static currentUser: any = {username: '', globalAdmin: false};
    public static ADMIN_ROLE = "ROLE_ADMIN";
}
interface MessageBody {
    server: string;
    serverType: string;
    msgType: string;
    text: string;
}

const requestFinishCallback = (resp: any) => {
    if (resp.resultCode !== 0) {
        CommonNotice.error(ErrorUtil.formatErrResp(resp));
    }
};

export {JarBootConst, MessageBody, requestFinishCallback};
