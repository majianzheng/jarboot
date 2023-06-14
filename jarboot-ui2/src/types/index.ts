import type { FuncCode } from '@/common/EventConst';

export type SelectOption = {
  label: string;
  value: any;
  children?: SelectOption[];
};

export type SearchConfig = {
  prop: string;
  name?: string;
  placeholder?: string;
  startPlaceholder?: string;
  endPlaceholder?: string;
  pickerOptions?: any;
  defaultTime?: any;
  type: 'input' | 'input-number' | 'single-selection' | 'multi-selection' | 'daterange' | 'datetimerange' | 'date' | 'switch' | 'cascader';
  options?: SelectOption[];
};

export type ResponseVo = {
  success: boolean;
  msg: string;
  data: any;
  errorCode: number;
  errStack: string;
  extraData: any;
};

export type SysUser = {
  id?: string;
  username: string;
  fullName: string;
  roles: string;
  userDir: string;
  password?: string;
};

export type RoleInfo = {
  id?: string;
  role: string;
  name: string;
};

export type Privilege = {
  role: string;
  authCode: string;
  permission: boolean;
};

export type FileNode = {
  name: string;
  parent?: string;
  key: string;
  directory: boolean;
  progress: number | null;
  size?: number;
  modifyTime: number;
  children?: FileNode[];
};

export type ServerSetting = {
  applicationType: 'java' | 'shell' | 'executable';
  args: string;
  command: string;

  scheduleType: string;
  daemon: boolean;
  env: string;
  group: string;
  jarUpdateWatch: boolean;
  jdkPath: string;
  lastModified: number;
  name: string;
  priority: number;
  sid: string;
  vm: string;
  vmContent: string;
  workDirectory: string;
  workspace: string;
  serviceDir: FileNode;
};
export type GlobalSetting = {
  workspace: string;
  defaultVmOptions: string;
};

export type MsgData = {
  event: number;
  sid: string;
  body: any;
};
export type MsgReq = {
  service?: string;
  sid?: string;
  body: string;
  func: FuncCode;
};
export enum CONSOLE_TOPIC {
  APPEND_LINE,
  STD_PRINT,
  BACKSPACE,
  FINISH_LOADING,
  INSERT_TO_HEADER,
  START_LOADING,
  CLEAR_CONSOLE,
  SCROLL_TO_END,
  SCROLL_TO_TOP,
}
export interface TreeNode {
  sid?: string;
  title?: string;
  key?: string;
  selectable?: boolean;
}

export interface ServiceInstance extends TreeNode {
  name: string;
  status?: string;
  group?: string;
  path?: string;

  onlineDebug: boolean;

  attaching: boolean;

  pid: number;

  remote: boolean;

  attached: boolean;
  children?: ServiceInstance[];
}

export interface JvmProcess extends TreeNode {
  fullName?: string;
  pid: number;
  attached: boolean;
  remote: string;
  attaching: boolean;
  trusted: boolean;
  children?: JvmProcess[];
}
