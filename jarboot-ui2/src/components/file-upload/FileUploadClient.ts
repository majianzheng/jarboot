import type { UploadFileInfo } from '@/types';
import CommonNotice from '@/common/CommonNotice';
import { defer } from 'lodash';
import Logger from '@/common/Logger';
import { Base64 } from '@/common/Base64';

const UPLOAD_CHUNK_SIZE = 4000;

/**
 * 文件上传客户端
 */
export default class FileUploadClient {
  private readonly file: File;
  private readonly dstPath: string;
  private readonly filename: string;
  private readonly relativePath: string;
  private readonly totalSize: number;
  private readonly clusterHost: string;
  private readonly speed: number;
  private readonly sendCountOnce: number = 2622;
  private readonly baseDir: string = '';
  private websocket: WebSocket | null = null;
  private paused: boolean = false;
  private progressHandlers: ((event: UploadFileInfo) => void)[] = [];
  private finishedHandlers: ((event: UploadFileInfo) => void)[] = [];
  private lastUploadProgress: UploadFileInfo | null = null;
  private startTime: number = 0;
  private uploadMode: string = '';

  /**
   * 文件上传客户端构造函数
   * <br>
   * 目的文件路径规则：
   * 1、workspace不为空，则为 workspace / path / file.webkitRelativePath
   * 2、workspace为空，则为 JARBOOT_HOME / path / file.webkitRelativePath
   * @param file 上传的文件
   * @param uploadMode 模式 home、service、 workspace
   * @param baseDir 工作空间
   * @param path 上传路径
   * @param clusterHost 集群机器
   * @param speed 速度限制，单位MB
   */
  public constructor(
    file: File,
    uploadMode: 'home' | 'service' | 'workspace' | '',
    baseDir: string,
    path: string,
    clusterHost: string,
    speed: number = 10
  ) {
    this.file = file;
    this.filename = file.name;
    this.relativePath = file.webkitRelativePath;
    this.totalSize = file.size;
    this.clusterHost = clusterHost || '';
    this.uploadMode = uploadMode;
    this.baseDir = baseDir;
    this.dstPath = path + '/' + (file.webkitRelativePath || file.name);
    if (!speed || speed <= 0) {
      this.speed = 10;
    } else {
      this.speed = speed;
    }
    this.sendCountOnce = Math.ceil((this.speed * 1024 * 1024) / UPLOAD_CHUNK_SIZE);
  }

  public addUploadEventHandler(callback: (event: UploadFileInfo) => void) {
    this.progressHandlers.push(callback);
  }

  public addFinishedEventHandler(callback: (event: UploadFileInfo) => void) {
    this.finishedHandlers.push(callback);
  }

  public getKey() {
    return `${this.clusterHost || 'localhost'}://${this.dstPath}`;
  }

  /**
   * 开始上传
   */
  public async upload() {
    // 开始上传
    const params = {
      filename: this.filename,
      totalSize: this.totalSize,
      dstPath: this.dstPath,
      uploadMode: this.uploadMode,
      baseDir: this.baseDir,
      clusterHost: this.clusterHost,
      sendCountOnce: this.sendCountOnce,
      relativePath: this.relativePath,
    } as any;
    if (null == this.websocket) {
      const query = 'params=' + Base64.base64UrlEncode(JSON.stringify(params));
      const protocol = 'https:' === window.location.protocol ? 'wss' : 'ws';
      const url = `${protocol}://${this.getDefaultHost()}/jarboot/upload/ws?${query}`;

      Logger.log('文件上传，开始连接:' + url);
      this.websocket = new WebSocket(url);
      this.websocket.onopen = () => Logger.log(`连接成功，等待文件上传！`);
      this.websocket.onmessage = (event: MessageEvent) => this.handleMessage(event);
      this.websocket.onerror = error => {
        this.websocket = null;
        Logger.error(error);
      };
      this.websocket.onclose = event => {
        this.websocket = null;
        Logger.log('文件上传服务，连接关闭！', event);
      };
      this.startTime = Date.now();
    }
    if (this.paused) {
      this.paused = false;
      if (this.lastUploadProgress) {
        this.lastUploadProgress.pause = false;
        this.triggerProgressChange(this.lastUploadProgress);
      }
      this.sendFile();
    }
  }

  /**
   * 暂停上传
   */
  public pause() {
    if (this.paused) {
      return;
    }
    this.paused = true;
    if (this.lastUploadProgress) {
      this.lastUploadProgress.pause = true;
      this.triggerProgressChange(this.lastUploadProgress);
    }
  }

  private sendFile() {
    if (!this.lastUploadProgress || this.paused) {
      return;
    }
    if (this.lastUploadProgress.uploadSize === this.lastUploadProgress.totalSize) {
      return;
    }

    for (let i = 0; i < this.sendCountOnce; ++i) {
      const chunkEnd = this.lastUploadProgress.uploadSize + UPLOAD_CHUNK_SIZE;
      const end = chunkEnd >= this.file.size ? this.file.size : chunkEnd;
      if (this.lastUploadProgress.uploadSize >= end) {
        return;
      }
      const data = this.file.slice(this.lastUploadProgress.uploadSize, end);
      this.websocket?.send(data);
      const updateSize = this.lastUploadProgress.uploadSize + data.size;
      if (this.lastUploadProgress && updateSize >= this.lastUploadProgress.totalSize) {
        return;
      }
      this.lastUploadProgress.uploadSize = updateSize;
    }
  }

  private getDefaultHost() {
    return import.meta.env.DEV ? `${window.location.hostname}:9899` : `${window.location.host}`;
  }

  private handleMessage(event: MessageEvent) {
    const lastUploadProgress = JSON.parse(event.data) as UploadFileInfo;
    lastUploadProgress.pause = this.paused;
    if (!this.lastUploadProgress) {
      this.lastUploadProgress = lastUploadProgress;
      // 开始发送数据
      defer(() => this.sendFile());
    } else if (lastUploadProgress.event === 'send') {
      defer(() => this.sendFile());
    }
    this.triggerProgressChange(lastUploadProgress);
    if (lastUploadProgress.errorMsg) {
      CommonNotice.warn('文件传输异常：' + lastUploadProgress.errorMsg);
      Logger.error(lastUploadProgress.errorMsg);
      this.paused = true;
      return;
    }
    if (lastUploadProgress.uploadSize >= lastUploadProgress.totalSize) {
      this.lastUploadProgress = lastUploadProgress;
      this.finished();
    }
  }

  private triggerProgressChange(lastUploadProgress: UploadFileInfo) {
    this.progressHandlers.forEach(callback => callback(lastUploadProgress));
  }

  private finished() {
    this.finishedHandlers.forEach(callback => callback(this.lastUploadProgress as UploadFileInfo));
    const costTime = Date.now() - this.startTime;
    Logger.log(`传输完成，耗时：${costTime / 1000} 秒`);
    this.websocket?.close();
    this.websocket = null;
  }
}
