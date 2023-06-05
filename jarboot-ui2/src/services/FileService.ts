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
  public static getFiles(baseDir: string, withRoot: boolean) {
    const form = new FormData();
    form.append('baseDir', baseDir);
    form.append('withRoot', withRoot + '');
    return Request.post<FileNode[]>(`${urlBase}/list`, form);
  }

  /**
   * 获取文件内容
   * @param path
   */
  public static getContent(path: string) {
    const form = new FormData();
    form.append('path', path);
    return Request.post<string>(`${urlBase}/file/text`, form);
  }

  /**
   * 删除文件
   * @param path
   */
  public static deleteFile(path: string) {
    const form = new FormData();
    form.append('path', path);
    return Request.post<string>(`${urlBase}/file/delete`, form);
  }

  /**
   * 写文件
   * @param path
   * @param content
   */
  public static writeFile(path: string, content: string) {
    const form = new FormData();
    form.append('path', path);
    form.append('content', content);
    return Request.post<string>(`${urlBase}/text`, form);
  }

  /**
   * 新建文件
   * @param path
   * @param content
   */
  public static newFile(path: string, content: string) {
    const form = new FormData();
    form.append('path', path);
    form.append('content', content);
    return Request.post<string>(`${urlBase}/text/create`, form);
  }

  /**
   * 新增文件夹
   * @param path
   */
  public static addDirectory(path: string) {
    const form = new FormData();
    form.append('path', path);
    return Request.post<string>(`${urlBase}/directory`, form);
  }

  public static download(path: string, name: string, callback: (result: boolean, msg?: string) => void) {
    const form = new FormData();
    form.append('path', path);
    CommonUtils.download(`${urlBase}/file/download`, name, 'POST', form, callback);
  }
}
