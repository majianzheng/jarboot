/**
 * 通用常量定义
 * @author majianzheng
 */
export const DOCS_URL = 'https://www.yuque.com/jarboot/usage/quick-start';
export const PROTOCOL_SPLIT = '\r';

export const CONTENT_VIEW = 'contentView';
export const CONFIG_VIEW = 'config';
export const CONSOLE_VIEW = 'console';

//进程状态
export const STATUS_STARTED = 'RUNNING';
export const STATUS_STOPPED = 'STOPPED';
export const STATUS_STARTING = 'STARTING';
export const STATUS_STOPPING = 'STOPPING';
export const STATUS_SCHEDULING = 'SCHEDULING';
export const STATUS_ATTACHED = 'ATTACHED';
export const STATUS_NOT_ATTACHED = 'NOT_ATTACHED';

//Online debug
export const ATTACHING = 'ATTACHING';
export const ATTACHED = STATUS_ATTACHED;
export const EXITED = 'EXITED';
export const NOT_TRUSTED = 'NOT_TRUSTED';
export const TRUSTED = 'TRUSTED';

export const HIGHLIGHT_STYLE = { backgroundColor: '#ffc069', padding: 0 };

export const ZH_CN = 'zh-CN';

//token
export const TOKEN_KEY = 'token';
export const ACCESS_CLUSTER_HOST = 'Access-Cluster-Host';
export const currentUser: any = { username: '', globalAdmin: false };
export const ADMIN_ROLE = 'ROLE_ADMIN';
export const SYS_ROLE = 'ROLE_SYS';

export const LOCALHOST = 'localhost';

export const DEFAULT_PRIVILEGE = {
  SERVICES_MGR: true,
  ONLINE_DEBUG: true,
  TOOLS: true,
  FILE_MGR: true,
};
