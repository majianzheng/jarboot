import { NotifyType, WsManager } from '@/common/WsManager';
import { MSG_EVENT } from '@/common/EventConst';
import type { MsgData } from '@/types';
import Logger from '@/common/Logger';
import { CONSOLE_TOPIC } from '@/types';
import CommonNotice from '@/common/CommonNotice';
import type PublishSubmit from '@/common/PublishSubmit';

/**
 * 服务订阅发布实现
 */
const TOPIC_SPLIT = '\r';

enum PUB_TOPIC {
  ROOT = 'root',
  CMD_END = 'commandEnd',
  RENDER_JSON = 'renderJson',
  QUICK_EXEC_CMD = 'quickExecCmd',
  RECONNECTED = 'reconnected',
  WORKSPACE_CHANGE = 'workspaceChange',
  STATUS_CHANGE = 'statusChange',
  FOCUS_CMD_INPUT = 'focusCmdInput',
  ONLINE_DEBUG_EVENT = 'onlineDebugEvent',
  NOT_TRUSTED = 'notTrusted',
}

class ServerPubsubImpl implements PublishSubmit {
  private handlers = new Map<string, Set<(data: any) => void>>();

  constructor() {
    WsManager.addMessageHandler(MSG_EVENT.STD_PRINT, this.stdPrint);
    WsManager.addMessageHandler(MSG_EVENT.SERVER_STATUS, this.statusChange);
    WsManager.addMessageHandler(MSG_EVENT.JVM_PROCESS_CHANGE, this.onJvmProcessChange);
  }

  public init() {
    WsManager.addMessageHandler(MSG_EVENT.NOTICE, this.notify);
    WsManager.addMessageHandler(WsManager.RECONNECTED_EVENT, this.onReconnected);
    WsManager.addMessageHandler(MSG_EVENT.WORKSPACE_CHANGE, this.workspaceChange);
  }

  private static genTopicKey(namespace: string, event: string | number) {
    return `${namespace}${TOPIC_SPLIT}${event}`;
  }

  public publish(namespace: string, event: string | number, data?: any): void {
    const key = ServerPubsubImpl.genTopicKey(namespace, event);
    const sets = this.handlers.get(key);
    if (sets?.size) {
      sets.forEach(handler => handler && handler(data));
    }
  }

  public submit(namespace: string, event: string | number, handler: (data: any) => void): void {
    const key = ServerPubsubImpl.genTopicKey(namespace, event);
    let sets = this.handlers.get(key);
    if (sets?.size) {
      sets.add(handler);
    } else {
      sets = new Set<(data: any) => void>();
      sets.add(handler);
      this.handlers.set(key, sets);
    }
  }

  public unSubmit(namespace: string, event: string | number, handler: (data: any) => void): void {
    const key = ServerPubsubImpl.genTopicKey(namespace, event);
    const sets = this.handlers.get(key);
    if (sets?.size) {
      sets.delete(handler);
      if (sets.size === 0) {
        this.handlers.delete(key);
      }
    }
  }

  private stdPrint = (data: MsgData) => {
    this.publish(data.sid, CONSOLE_TOPIC.STD_PRINT, data.body);
  };

  private workspaceChange = (data: MsgData) => {
    this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.WORKSPACE_CHANGE, data.body);
    Logger.log(`工作空间已经被修改，服务列表将会被刷新！`);
  };

  private notify = (data: MsgData) => {
    const body: string = data.body;
    const success = '0' === body[0];
    const index = body.indexOf(',');
    const type = parseInt(body.substring(1, index));
    const msg = body.substring(index + 1);
    switch (type) {
      case NotifyType.INFO:
        CommonNotice.success(msg);
        Logger.log(msg);
        break;
      case NotifyType.WARN:
        CommonNotice.warn(msg);
        Logger.warn(msg);
        break;
      case NotifyType.ERROR:
        CommonNotice.error(msg);
        Logger.error(msg);
        break;
      case NotifyType.CONSOLE:
        this.publish(data.sid, CONSOLE_TOPIC.APPEND_LINE, msg);
        break;
      case NotifyType.COMMAND_END:
        if (success) {
          this.publish(data.sid, PUB_TOPIC.CMD_END, msg);
        } else {
          this.publish(data.sid, PUB_TOPIC.CMD_END, `[31m${msg}[0m`);
        }
        break;
      case NotifyType.JSON_RESULT:
        this.renderCmdJsonResult(data.sid, msg);
        break;
      default:
        CommonNotice.error(`Notify type error: ${type}`, msg);
        Logger.log('未知的type', body);
        break;
    }
  };

  private statusChange = (data: MsgData) => {
    this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.STATUS_CHANGE, data);
  };

  private onReconnected = (data: MsgData) => {
    this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.RECONNECTED, data.body);
    Logger.log(`重新连接服务成功，服务列表将会被刷新！`);
  };

  private onJvmProcessChange = (data: MsgData) => {
    this.publish(PUB_TOPIC.ROOT, PUB_TOPIC.ONLINE_DEBUG_EVENT, data);
  };

  private renderCmdJsonResult = (sid: string, body: string) => {
    if ('{' !== body[0]) {
      //不是json数据时，使用console
      Logger.warn(`当前非JSON数据格式！`, body);
      this.publish(sid, CONSOLE_TOPIC.APPEND_LINE, body);
      return;
    }
    body = JSON.parse(body);
    this.publish(sid, PUB_TOPIC.RENDER_JSON, body);
  };
}

const pubsub: PublishSubmit = new ServerPubsubImpl();

export { pubsub, PUB_TOPIC };
