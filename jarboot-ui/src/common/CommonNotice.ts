import { notification } from 'antd';

/**
 * 通知组件
 */
export default class CommonNotice {
  /**
   * 成功通知
   * @param msg 消息
   * @param description 描述
   */
  static success(msg: string, description = '') {
    notification['success']({
      message: msg,
      description: description,
    });
  }

  /**
   * 消息通知
   * @param msg 消息
   * @param description 描述
   */
  static info(msg: string, description = '') {
    notification['info']({
      message: msg,
      description: description,
    });
  }

  /**
   * 错误通知
   * @param msg 消息
   * @param description 描述
   */
  static error = (msg: string, description = '') => {
    notification['error']({
      message: msg,
      description: description,
    });
  };

  /**
   * 警告通知
   * @param msg 消息
   * @param description 描述
   */
  static warn = (msg: string, description = '') => {
    notification['warn']({
      message: msg,
      description: description,
    });
  };
}
