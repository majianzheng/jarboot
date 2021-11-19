import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";

class JarBootConst {
    public static DOCS_URL = "https://www.yuque.com/jarboot/usage/quick-start";
    public static PROTOCOL_SPLIT = '\r';

    //进程状态
    public static STATUS_STARTED = 'RUNNING';
    public static STATUS_STOPPED = 'STOPPED';
    public static STATUS_STARTING = 'STARTING';
    public static STATUS_STOPPING = 'STOPPING';

    public static NOTICE_INFO = 0;
    public static NOTICE_WARN = 1;
    public static NOTICE_ERROR = 2;

    public static PANEL_HEIGHT = (window.innerHeight - 150);

    public static ZH_CN = 'zh-CN';

    public static APPEND_LINE = 'appendLine';
    public static PRINT = 'print';
    public static BACKSPACE = 'backspace';
    public static BACKSPACE_LINE = 'backspaceLine';
    public static FINISH_LOADING = 'finishLoading';
    public static INSERT_TO_HEADER = 'insertLineToHeader';
    public static START_LOADING = 'startLoading';
    public static CLEAR_CONSOLE = 'clear';

    //token
    public static TOKEN_KEY = 'token';
    public static currentUser: any = {username: '', globalAdmin: false};
    public static ADMIN_ROLE = "ROLE_ADMIN";
}

interface MsgData {
    event: number,
    sid: string,
    body: any
}

const requestFinishCallback = (resp: any) => {
    if (resp.resultCode !== 0) {
        CommonNotice.error(ErrorUtil.formatErrResp(resp));
    }
};

export {JarBootConst, MsgData, requestFinishCallback};
