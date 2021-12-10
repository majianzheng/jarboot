import CommonNotice from "@/common/CommonNotice";
import ErrorUtil from "@/common/ErrorUtil";

/**
 * 通用常量定义
 * @author majianzheng
 */
class JarBootConst {
    public static readonly DOCS_URL = "https://www.yuque.com/jarboot/usage/quick-start";
    public static readonly PROTOCOL_SPLIT = '\r';

    public static readonly  SIDE_VIEW = 'sideView';
    public static readonly CONTENT_VIEW = 'contentView';
    public static readonly TREE_VIEW = 'tree';
    public static readonly LIST_VIEW = 'list';
    public static readonly CONFIG_VIEW = 'config';
    public static readonly CONSOLE_VIEW = 'console';

    //进程状态
    public static readonly STATUS_STARTED = 'RUNNING';
    public static readonly STATUS_STOPPED = 'STOPPED';
    public static readonly STATUS_STARTING = 'STARTING';
    public static readonly STATUS_STOPPING = 'STOPPING';

    //Online debug
    public static readonly ATTACHING = 'ATTACHING';
    public static readonly ATTACHED = 'ATTACHED';
    public static readonly EXITED = 'EXITED';

    public static PANEL_HEIGHT = (window.innerHeight - 62);
    public static HIGHLIGHT_STYLE = {backgroundColor: '#ffc069', padding: 0};

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

    public static readonly IS_SAFARI = window.hasOwnProperty('safari');

    public static readonly LOCALHOST = 'localhost';
}

interface MsgData {
    event: number;
    sid: string;
    body: any;
}

interface MsgReq {
    server?: string;
    sid?: string;
    body: string;
    func: number;
}

const requestFinishCallback = (resp: any) => {
    if (resp.resultCode !== 0) {
        CommonNotice.error(ErrorUtil.formatErrResp(resp));
    }
};

export {JarBootConst, MsgData, MsgReq, requestFinishCallback};
