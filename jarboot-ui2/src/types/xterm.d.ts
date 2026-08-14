import type {} from '@xterm/xterm';

declare module '@xterm/xterm' {
  interface Terminal {
    /** 自定义扩展：提示符渲染 */
    prompt: () => void;
    /** 自定义扩展：设置当前输入 */
    setCurrent: (str: string) => void;
    /** xterm 内部实例 */
    _core: any;
  }
}
