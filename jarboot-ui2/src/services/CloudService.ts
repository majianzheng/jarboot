import Request from '@/common/Request';
import Logger from '@/common/Logger';

const urlBase = '/api/jarboot/cloud';

/**
 * Cloud service
 */
export default class CloudService {
  public static pushServerDirectory(file: File) {
    const form: FormData = new FormData();
    if (file) {
      form.append('file', file);
    } else {
      Logger.error('file is null.', file);
      return Promise.reject('file is null');
    }
    return Request.upload(`${urlBase}/push/server`, file);
  }
}
