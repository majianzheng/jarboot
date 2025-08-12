import Request from '@/common/Request';
import type { FileNode } from '@/types';
import CommonUtils from '@/common/CommonUtils';

const urlBase = '/api/jarboot/file-manager';

/**
 * File service
 */
export default class FileService {
  /**
   * 获取版本
   * @param baseDir
   * @param withRoot
   * @returns {Promise<any>}
   */
  public static getFiles(baseDir: string, withRoot: boolean, clusterHost?: string): Promise<any> {
    const form = new FormData();
    form.append('baseDir', baseDir);
    if (clusterHost) {
      form.append('clusterHost', clusterHost);
    }
    form.append('withRoot', withRoot + '');
    return Request.post<FileNode[]>(`${urlBase}/list`, form);
  }

  /**
   * 获取文件内容
   * @param path
   */
  public static getContent(path: string, clusterHost?: string) {
    const form = new FormData();
    form.append('path', path);
    if (clusterHost) {
      form.append('clusterHost', clusterHost);
    }
    return Request.post<string>(`${urlBase}/file/text`, form);
  }

  /**
   * 解析路径
   * @param node
   * @param baseDir
   */
  public static parseFilePath(node: any, baseDir: string) {
    let path = node.data.name;
    let parent = node.parent;
    if (!node.data?.parent) {
      return baseDir;
    }
    while (parent?.data.parent) {
      path = parent.data.name + '/' + path;
      parent = parent.parent;
    }
    if (baseDir) {
      path = baseDir + '/' + path;
    }
    return path;
  }

  /**
   * 删除文件
   * @param path
   * @param clusterHost
   */
  public static deleteFile(path: string, clusterHost?: string) {
    return Request.delete<string>(`${urlBase}/file/delete?path=${path}&clusterHost=${clusterHost ?? ''}`, {});
  }

  /**
   * 写文件
   * @param path
   * @param content
   */
  public static writeFile(path: string, content: string, clusterHost?: string) {
    const form = new FormData();
    form.append('path', path);
    form.append('content', content);
    if (clusterHost) {
      form.append('clusterHost', clusterHost);
    }
    return Request.post<string>(`${urlBase}/text`, form);
  }

  /**
   * 新建文件
   * @param path
   * @param content
   */
  public static newFile(path: string, content: string, clusterHost?: string) {
    const form = new FormData();
    form.append('path', path);
    form.append('content', content);
    if (clusterHost) {
      form.append('clusterHost', clusterHost);
    }
    return Request.post<string>(`${urlBase}/text/create`, form);
  }

  /**
   * 新增文件夹
   * @param path
   */
  public static addDirectory(path: string, clusterHost?: string) {
    const form = new FormData();
    form.append('path', path);
    if (clusterHost) {
      form.append('clusterHost', clusterHost);
    }
    return Request.post<string>(`${urlBase}/directory`, form);
  }

  public static download(path: string, name: string, callback: (result: boolean, msg?: string) => void, clusterHost?: string) {
    const form = new FormData();
    form.append('path', path);
    if (clusterHost) {
      form.append('clusterHost', clusterHost);
    }
    CommonUtils.download(`${urlBase}/file/download`, name, 'POST', form, callback);
  }
}
