import { ElNotification } from 'element-plus';

/**
 * 通知组件
 * @author majianzheng
 */
export default class CommonNotice {
  /**
   * 成功通知
   * @param msg 消息
   * @param description 描述
   */
  static success(msg: string, description = '') {
    ElNotification({
      title: msg,
      message: description,
      type: 'success',
    });
  }

  /**
   * 消息通知
   * @param msg 消息
   * @param description 描述
   */
  static info(msg: string | any, description: any = '') {
    ElNotification({
      title: msg,
      message: description,
      type: 'info',
    });
  }

  /**
   * 错误通知
   * @param msg 消息
   * @param description 描述
   */
  static error = (msg: string | any, description: any = '') => {
    ElNotification({
      title: msg,
      message: description,
      type: 'error',
    });
  };

  /**
   * 警告通知
   * @param msg 消息
   * @param description 描述
   */
  static warn = (msg: string | any, description: any = '') => {
    ElNotification({
      title: msg,
      message: description,
      type: 'warning',
    });
  };
}
