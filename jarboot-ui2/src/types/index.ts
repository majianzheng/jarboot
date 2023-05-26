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
  role: number;
  relName?: string;
  studentCode?: string;
  groupId?: string;
  groupName?: string;
  status?: number;
  password?: string;
  rePassword?: string;
  updatedPassword?: boolean;
  license?: boolean;
};

export interface UserLoginDto extends SysUser {
  accessToken: string;
  tokenTtl: number;
}

export type UserGroup = {
  id?: string;
  name: string;
  internal?: boolean;
  createTime: number;
  count?: number;
};

export type BasicInfoDto = {
  version: string;
  productName: string;
  machineCode: string;
  licenseStartTime: number;
  licenseEndTime: number;
  licenseState: number;
};
