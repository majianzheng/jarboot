export enum EventType {
  /** Console一行 */
  CONSOLE_EVENT,
  /** Std标准输出 */
  STD_PRINT_EVENT,
  /** std退格 */
  BACKSPACE_EVENT,
  /** 清屏 */
  CLEAR_EVENT,
}

export type ConsoleEvent = {
  /** 事件类型 */
  type: EventType;
  /** 文本 */
  text?: string;
  /** 退格次数 */
  backspaceNum?: number;
};

export type SgrOption = {
  /** 前景色 */
  foregroundColor: string;
  /** 背景色 */
  backgroundColor: string;
  /** 是否粗体 */
  bold: boolean;
  /** 是否弱化 */
  weaken: boolean;
  /** 是否因此 */
  hide: boolean;
  /** 反显，前景色和背景色掉换 */
  exchange: boolean;
  /** 倾斜 */
  oblique: boolean;
  /** 下划线 */
  underline: boolean;
  /** 上划线 */
  overline: boolean;
  /** 贯穿线 */
  through: boolean;
  /** 缓慢闪烁 */
  slowBlink: boolean;
  /** 快速闪烁 */
  fastBlink: boolean;
};
