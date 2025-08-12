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
  avatar: string;
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
  leaf: boolean;
  children?: FileNode[];
};

export type ServerSetting = {
  host: string;
  applicationType: 'java' | 'shell' | 'executable';
  args: string;
  command: string;

  scheduleType: string;
  cron: string;
  daemon: boolean;
  env: string;
  group: string;
  fileUpdateWatch: boolean;
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
  jdkPath: string;
  servicesAutoStart: boolean;
  maxStartTime: number;
  maxExitTime: number;
  afterServerOfflineExec: string;
  fileChangeShakeTime: number;
};

export type MsgData = {
  event: number;
  sid: string;
  body: any;
};
export type MsgReq = {
  host: string;
  service?: string;
  sid?: string;
  body: string;
  func: FuncCode;
  cols: number;
  rows: number;
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
  isLeaf: boolean;
}

export interface ServiceInstance extends TreeNode {
  host: string;
  hostName: string;
  name: string;
  status: string;
  group?: string;
  path?: string;
  attaching: boolean;
  nodeType: number;
  pid: string;

  remote: string;
  trusted: boolean;
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

export type ServerRuntimeInfo = {
  version: string;
  uuid: string;
  host: string;
  workspace: string;
  masterHost: string;
  inDocker: boolean;
  dev: boolean;
  os: string;
  jdk: string;
  machineCode: string;
};
export type UploadFileInfo = {
  id: string;
  filename: string;
  clusterHost: string;
  md5: string;
  dstPath: string;
  totalSize: number;
  pause: boolean;
  uploadSize: number;
  errorMsg?: string;
  event?: string;
};
export type MenuItem = {
  name: string;
  path: string;
  module?: string;
  code?: string;
  icon?: string;
  subName?: string;
  children?: MenuItem[];
};
