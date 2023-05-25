/**
 * 通用常量定义
 * @author majianzheng
 */
export default class CommonConst {
  public static readonly DOCS_URL = 'https://www.yuque.com/jarboot/usage/quick-start';
  public static readonly PROTOCOL_SPLIT = '\r';

  public static readonly CONTENT_VIEW = 'contentView';
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
  public static readonly NOT_TRUSTED = 'NOT_TRUSTED';
  public static readonly TRUSTED = 'TRUSTED';

  public static PANEL_HEIGHT = window.innerHeight - 62;
  public static HIGHLIGHT_STYLE = { backgroundColor: '#ffc069', padding: 0 };

  public static ZH_CN = 'zh-CN';

  //token
  public static TOKEN_KEY = 'token';
  public static currentUser: any = { username: '', globalAdmin: false };
  public static ADMIN_ROLE = 'ROLE_ADMIN';

  public static readonly LOCALHOST = 'localhost';
}
