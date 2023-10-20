import SparkMD5 from 'spark-md5';
import CommonUtils from '@/common/CommonUtils';
import { ACCESS_CLUSTER_HOST } from '@/common/CommonConst';
import type { UploadFileInfo } from '@/types';
import CommonNotice from '@/common/CommonNotice';
import { defer } from 'lodash';
import { ElLoading } from 'element-plus';

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
  private md5: string = '';
  private websocket: WebSocket | null = null;
  private paused: boolean = false;
  private progressHandlers: ((event: UploadFileInfo) => void)[] = [];
  private finishedHandlers: ((event: UploadFileInfo) => void)[] = [];
  private lastUploadProgress: UploadFileInfo | null = null;
  private startTime: number = 0;
  private importService: string = '';

  /**
   * 文件上传客户端构造函数
   * <br>
   * 目的文件路径规则：
   * 1、workspace不为空，则为 workspace / path / file.webkitRelativePath
   * 2、workspace为空，则为 JARBOOT_HOME / path / file.webkitRelativePath
   * @param file 上传的文件
   * @param baseDir 工作空间
   * @param path 上传路径
   * @param clusterHost 集群机器
   * @param speed 速度限制，单位MB
   */
  public constructor(file: File, baseDir: string, path: string, clusterHost: string, speed: number = 10) {
    this.file = file;
    this.filename = file.name;
    this.relativePath = file.webkitRelativePath;
    this.totalSize = file.size;
    this.clusterHost = clusterHost || '';
    this.baseDir = baseDir;
    this.dstPath = path + '/' + (file.webkitRelativePath || file.name);
    if (!speed || speed <= 0) {
      this.speed = 10;
    } else {
      this.speed = speed;
    }
    this.sendCountOnce = Math.ceil((this.speed * 1024 * 1024) / UPLOAD_CHUNK_SIZE);
  }

  public setImportService(s: string) {
    this.importService = s;
  }

  public addUploadEventHandler(callback: (event: UploadFileInfo) => void) {
    this.progressHandlers.push(callback);
  }

  public addFinishedEventHandler(callback: (event: UploadFileInfo) => void) {
    this.finishedHandlers.push(callback);
  }

  public getDstPath() {
    return this.dstPath;
  }

  /**
   * 开始上传
   */
  public async upload() {
    if (!this.md5) {
      const loading = ElLoading.service({ fullscreen: true, text: this.filename + ' 加载中...' });
      this.md5 = await this.getFileMd5(this.file);
      loading.close();
    }
    // 开始上传
    if (null == this.websocket) {
      let query = `md5=${this.md5}&filename=${encodeURIComponent(this.filename)}`;
      query += `&totalSize=${this.totalSize}&dstPath=${encodeURIComponent(this.dstPath)}`;
      const clusterHost = CommonUtils.getCurrentHost();
      if (this.relativePath) {
        query += `&relativePath=${encodeURIComponent(this.relativePath)}`;
      }
      if (this.clusterHost) {
        query += `&clusterHost=${this.clusterHost}`;
      }
      if (this.baseDir) {
        query += `&baseDir=${this.baseDir}`;
      }
      if (clusterHost) {
        query += `&${ACCESS_CLUSTER_HOST}=${clusterHost}`;
      }
      if (this.importService) {
        query += `&importService=${this.importService}`;
      }
      query += `&${CommonUtils.ACCESS_TOKEN}=${CommonUtils.getRawToken()}`;
      const url = `ws://${this.getDefaultHost()}/jarboot/upload/ws?${query}`;

      console.info('file upload connect to ' + url);
      this.websocket = new WebSocket(url);
      this.websocket.onopen = () => console.info(`文件上传 open url:${url}`);
      this.websocket.onmessage = (event: MessageEvent) => this.handleMessage(event);
      this.websocket.onerror = error => console.error(error);
      this.websocket.onclose = event => console.info(event);
      this.startTime = Date.now();
    }
    if (this.paused) {
      this.paused = false;
      if (this.lastUploadProgress) {
        this.lastUploadProgress.pause = false;
        this.triggerProgressChange();
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
      this.triggerProgressChange();
    }
  }

  private sendFile() {
    if (!this.lastUploadProgress || this.paused) {
      return;
    }
    if (this.lastUploadProgress.uploadSize === this.lastUploadProgress.totalSize) {
      this.finished();
      return;
    }

    for (let i = 0; i < this.sendCountOnce; ++i) {
      let chunkEnd = this.lastUploadProgress.uploadSize + UPLOAD_CHUNK_SIZE;
      let end = chunkEnd >= this.file.size ? this.file.size : chunkEnd;
      if (this.lastUploadProgress.uploadSize >= end) {
        this.finished();
        return;
      }
      const data = this.file.slice(this.lastUploadProgress.uploadSize, end);
      this.websocket?.send(data);
      this.lastUploadProgress.uploadSize += data.size;
      this.triggerProgressChange();
    }
    defer(() => this.sendFile());
  }

  private getDefaultHost() {
    return import.meta.env.DEV ? `${window.location.hostname}:9899` : `${window.location.host}`;
  }

  private handleMessage(event: MessageEvent) {
    this.lastUploadProgress = JSON.parse(event.data) as UploadFileInfo;
    this.triggerProgressChange();
    if (this.lastUploadProgress.errorMsg) {
      CommonNotice.warn('文件传输异常：' + this.lastUploadProgress.errorMsg);
      this.paused = true;
      return;
    }
    if (this.paused) {
      return;
    }
    // 发送下一段数据
    this.sendFile();
  }

  private triggerProgressChange() {
    this.progressHandlers.forEach(callback => callback(this.lastUploadProgress as UploadFileInfo));
  }

  private finished() {
    this.finishedHandlers.forEach(callback => callback(this.lastUploadProgress as UploadFileInfo));
    const costTime = Date.now() - this.startTime;
    console.info(`传输完成，耗时：${costTime / 1000} 秒`);
    this.websocket?.close();
    this.websocket = null;
  }

  getFileMd5(file: File): Promise<string> {
    const fileReader = new FileReader();
    const chunkSize1 = 102400;
    const chunks = Math.ceil(file.size / chunkSize1);
    let currentChunk = 0;
    const spark = new SparkMD5();
    return new Promise(resolve => {
      fileReader.onload = function (e) {
        if (!e.target?.result) {
          return;
        }
        spark.appendBinary(e.target.result as string);
        currentChunk++;
        if (currentChunk < chunks) {
          loadNext();
        } else {
          resolve(spark.end());
        }
      };
      fileReader.onerror = () => {
        console.log('文件读取失败，无法上传该文件');
      };

      function loadNext() {
        const start = currentChunk * chunkSize1;
        const end = start + chunkSize1 >= file.size ? file.size : start + chunkSize1;
        fileReader.readAsBinaryString(file.slice(start, end));
      }
      loadNext();
    });
  }
}
