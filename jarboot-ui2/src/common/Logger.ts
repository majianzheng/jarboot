/**
 * 日志
 * @author majianzheng
 */
export default class Logger {
  /**
   * 日志log
   * @param args
   */
  public static log(...args: any[]) {
    console.log(`%cJARBOOT`, 'font-weight:bold;color:green;border:1px solid gray;background:#b7eb8f', ...args);
  }

  /**
   * 错误log
   * @param args
   */
  public static error(...args: any[]) {
    console.error(`%cJARBOOT`, 'font-weight:bold;color:red;border:1px solid gray;background:#ffbb96', ...args);
  }

  /**
   * 警告log
   * @param args
   */
  public static warn(...args: any[]) {
    console.warn(`%cJARBOOT`, 'font-weight:bold;color:blue;border:1px solid gray;background:#ecd663', ...args);
  }
}
