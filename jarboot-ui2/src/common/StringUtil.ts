import moment from 'moment';
import { round } from 'lodash';

/**
 * 字符串工具类
 * @author majianzheng
 */
class StringUtil {
  /**
   * 判断是否为字符串
   * @param {*} object
   * @return {Boolean} isString
   */
  public static isString(object: any): boolean {
    return object instanceof String || typeof object === 'string' || (this.isNotNull(object) && object.constructor === String);
  }

  /**
   * 判断字符串是否为空
   * @param {String} str
   *
   */
  public static isEmpty(str: string | undefined | null) {
    if (null == str) {
      return true;
    }
    return this.isNull(str) || (this.isString(str) && str.trim() === '');
  }

  /**
   * 判断字符串是否不为空
   * @param {String} str
   *
   */
  public static isNotEmpty(str: string | undefined | null) {
    return !this.isEmpty(str);
  }

  /**
   * 判断是否为整数,默认验证非负整数
   * @param {String} str
   * @param positive
   * @return {Boolean} isNumber
   */
  public static isInt(str: string, positive = true): boolean {
    let exp = '^[0-9]*$';
    if (positive) {
      exp = '^\\d+$';
    } else if (!positive) {
      exp = '^((-\\d+)|(0+))$';
    }
    const reg = new RegExp(exp);
    return reg.test(str);
  }

  /**
   * 判断是否为浮点数,默认验证非负浮点数
   * @param {String} str
   * @param {Boolean} positive 是否为正数,true验证非负浮点,false验证非正浮点
   * @return {Boolean}
   */
  public static isFloat(str: string, positive: boolean = true): boolean {
    let exp = '^(-?\\d+)(\\.\\d+)?$';
    if (positive) {
      exp = '^\\d+(\\.\\d+)?$';
    } else if (!positive) {
      exp = '^((-\\d+(\\.\\d+)?)|(0+(\\.0+)?))$';
    }
    const reg = new RegExp(exp);
    return reg.test(str);
  }

  /**
   * 判断是否为数字(整数或浮点数),默认验证非负数
   * @param {String} str
   * @param {Boolean} positive 是否为正数,true验证非负浮点,false验证非正浮点
   * @return {Boolean}
   */
  public static isNumber(str: string, positive: boolean = true): boolean {
    //先验证是否为整数
    let valid = this.isInt(str, positive);
    //若不为整数,再验证是否为浮点数
    if (!valid) {
      valid = this.isFloat(str, positive);
    }
    return valid;
  }

  /**
   * 判断是否为数字(整数或浮点数),不区分正负
   * @param {String} str
   * @return {Boolean}
   */
  public static isNumberUnsigned(str: string): boolean {
    let valid = this.isNumber(str, true);
    if (!valid) {
      valid = this.isNumber(str, false);
    }
    return valid;
  }

  /**
   * 判断是否为null或undefined
   */
  public static isNull(obj: any) {
    return obj === null || obj === undefined;
  }

  /**
   * 判断是否不为null和undefined
   */
  public static isNotNull(obj: any) {
    return obj != null;
  }

  /**
   * 将值转换为boolean
   * 仅当值为1或true或"true"或"TRUE"时,返回true
   */
  public static toBoolean(val: any) {
    return this.isNotEmpty(val) && (val === 1 || val === '1' || val === true || val === 'true' || val === 'TRUE');
  }

  /**
   * 数组转为字符串,形式如: "1","2","3"
   *
   */
  public static arrayToString(arr: any, spliter = ',') {
    let str = '';
    if (this.isNotNull(arr)) {
      for (const value of arr) {
        str += spliter + value;
      }
      if (str.length > 0) {
        str = str.substring(1, str.length);
      }
    }
    return str;
  }

  /**
   * 判断是否为JSON字符串
   * @param {String} str
   */
  public static isJsonStr(str: any) {
    try {
      if (typeof JSON.parse(str) == 'object') {
        return true;
      }
    } catch (e) {
      // ignore
    }
    return false;
  }

  /**
   * 对过长的字符串进行截断处理
   * @param {String} str 原始字符串
   * @param {Number} targetLength 要显示的字符串长度
   */
  public static ellipsis(str: string, targetLength: number) {
    let result = str;
    if (str.length > targetLength) {
      result = str.substring(0, targetLength - 2) + '...';
    }
    return result;
  }

  /**
   * 时间戳格式化
   * @param time 时间戳
   * @param pattern YYYY/MM/DD hh:mm:ss
   */
  public static timeFormat(time: number, pattern = 'yyyy-MM-DD HH:mm:ss') {
    if (time) {
      const formatStr = moment(time).format(pattern);
      if (formatStr === 'Invalid date') {
        return time;
      } else {
        return formatStr;
      }
    } else {
      return '';
    }
  }

  public static formatBytes(sizeBytes: number) {
    let memoryUnits = [
      {
        unitName: 'bytes',
        threshold: 1024,
      },
      {
        unitName: 'KB',
        threshold: 1024,
      },
      {
        unitName: 'MB',
        threshold: 1024,
      },
      {
        unitName: 'GB',
        threshold: 1024,
      },
      {
        unitName: 'TB',
        threshold: 1024,
      },
    ];

    let tempFileSize = sizeBytes;
    let matchIndex = -1;

    for (let i = 0, end = false; i < memoryUnits.length; i++) {
      let memoryUnit = memoryUnits[i],
        end = i === memoryUnits.length - 1;
      if (tempFileSize <= memoryUnit.threshold || end) {
        matchIndex = i;
        break;
      }

      tempFileSize = tempFileSize / memoryUnit.threshold;
    }
    return {
      fileSize: round(tempFileSize, 2) + memoryUnits[matchIndex].unitName,
      matchUnit: memoryUnits[matchIndex],
      originalFileSize: sizeBytes,
    };
  }

  public static md5(string: string, bit: number) {
    function md5_RotateLeft(lValue: number, iShiftBits: number) {
      return (lValue << iShiftBits) | (lValue >>> (32 - iShiftBits));
    }
    function md5_AddUnsigned(lX: number, lY: number) {
      let lX4, lY4, lX8, lY8, lResult;
      lX8 = lX & 0x80000000;
      lY8 = lY & 0x80000000;
      lX4 = lX & 0x40000000;
      lY4 = lY & 0x40000000;
      lResult = (lX & 0x3fffffff) + (lY & 0x3fffffff);
      if (lX4 & lY4) {
        return lResult ^ 0x80000000 ^ lX8 ^ lY8;
      }
      if (lX4 | lY4) {
        if (lResult & 0x40000000) {
          return lResult ^ 0xc0000000 ^ lX8 ^ lY8;
        } else {
          return lResult ^ 0x40000000 ^ lX8 ^ lY8;
        }
      } else {
        return lResult ^ lX8 ^ lY8;
      }
    }
    function md5_F(x: number, y: number, z: number) {
      return (x & y) | (~x & z);
    }
    function md5_G(x: number, y: number, z: number) {
      return (x & z) | (y & ~z);
    }
    function md5_H(x: number, y: number, z: number) {
      return x ^ y ^ z;
    }
    function md5_I(x: number, y: number, z: number) {
      return y ^ (x | ~z);
    }
    function md5_FF(a: number, b: number, c: number, d: number, x: number, s: number, ac: number) {
      a = md5_AddUnsigned(a, md5_AddUnsigned(md5_AddUnsigned(md5_F(b, c, d), x), ac));
      return md5_AddUnsigned(md5_RotateLeft(a, s), b);
    }
    function md5_GG(a: number, b: number, c: number, d: number, x: number, s: number, ac: number) {
      a = md5_AddUnsigned(a, md5_AddUnsigned(md5_AddUnsigned(md5_G(b, c, d), x), ac));
      return md5_AddUnsigned(md5_RotateLeft(a, s), b);
    }
    function md5_HH(a: number, b: number, c: number, d: number, x: number, s: number, ac: number) {
      a = md5_AddUnsigned(a, md5_AddUnsigned(md5_AddUnsigned(md5_H(b, c, d), x), ac));
      return md5_AddUnsigned(md5_RotateLeft(a, s), b);
    }
    function md5_II(a: number, b: number, c: number, d: number, x: number, s: number, ac: number) {
      a = md5_AddUnsigned(a, md5_AddUnsigned(md5_AddUnsigned(md5_I(b, c, d), x), ac));
      return md5_AddUnsigned(md5_RotateLeft(a, s), b);
    }
    function md5_ConvertToWordArray(string: string) {
      let lWordCount;
      const lMessageLength = string.length;
      const lNumberOfWords_temp1 = lMessageLength + 8;
      const lNumberOfWords_temp2 = (lNumberOfWords_temp1 - (lNumberOfWords_temp1 % 64)) / 64;
      const lNumberOfWords = (lNumberOfWords_temp2 + 1) * 16;
      const lWordArray = Array(lNumberOfWords - 1);
      let lBytePosition = 0;
      let lByteCount = 0;
      while (lByteCount < lMessageLength) {
        lWordCount = (lByteCount - (lByteCount % 4)) / 4;
        lBytePosition = (lByteCount % 4) * 8;
        lWordArray[lWordCount] = lWordArray[lWordCount] | (string.charCodeAt(lByteCount) << lBytePosition);
        lByteCount++;
      }
      lWordCount = (lByteCount - (lByteCount % 4)) / 4;
      lBytePosition = (lByteCount % 4) * 8;
      lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition);
      lWordArray[lNumberOfWords - 2] = lMessageLength << 3;
      lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29;
      return lWordArray;
    }
    function md5_WordToHex(lValue: number) {
      let WordToHexValue = '',
        WordToHexValue_temp = '',
        lByte,
        lCount;
      for (lCount = 0; lCount <= 3; lCount++) {
        lByte = (lValue >>> (lCount * 8)) & 255;
        WordToHexValue_temp = '0' + lByte.toString(16);
        WordToHexValue = WordToHexValue + WordToHexValue_temp.substr(WordToHexValue_temp.length - 2, 2);
      }
      return WordToHexValue;
    }
    function md5_Utf8Encode(string: string) {
      string = string.replace(/\r\n/g, '\n');
      let utftext = '';
      for (let n = 0; n < string.length; n++) {
        const c = string.charCodeAt(n);
        if (c < 128) {
          utftext += String.fromCharCode(c);
        } else if (c > 127 && c < 2048) {
          utftext += String.fromCharCode((c >> 6) | 192);
          utftext += String.fromCharCode((c & 63) | 128);
        } else {
          utftext += String.fromCharCode((c >> 12) | 224);
          utftext += String.fromCharCode(((c >> 6) & 63) | 128);
          utftext += String.fromCharCode((c & 63) | 128);
        }
      }
      return utftext;
    }
    let x = [];
    let k, AA, BB, CC, DD, a, b, c, d;
    const S11 = 7,
      S12 = 12,
      S13 = 17,
      S14 = 22;
    const S21 = 5,
      S22 = 9,
      S23 = 14,
      S24 = 20;
    const S31 = 4,
      S32 = 11,
      S33 = 16,
      S34 = 23;
    const S41 = 6,
      S42 = 10,
      S43 = 15,
      S44 = 21;
    string = md5_Utf8Encode(string);
    x = md5_ConvertToWordArray(string);
    a = 0x67452301;
    b = 0xefcdab89;
    c = 0x98badcfe;
    d = 0x10325476;
    for (k = 0; k < x.length; k += 16) {
      AA = a;
      BB = b;
      CC = c;
      DD = d;
      a = md5_FF(a, b, c, d, x[k + 0], S11, 0xd76aa478);
      d = md5_FF(d, a, b, c, x[k + 1], S12, 0xe8c7b756);
      c = md5_FF(c, d, a, b, x[k + 2], S13, 0x242070db);
      b = md5_FF(b, c, d, a, x[k + 3], S14, 0xc1bdceee);
      a = md5_FF(a, b, c, d, x[k + 4], S11, 0xf57c0faf);
      d = md5_FF(d, a, b, c, x[k + 5], S12, 0x4787c62a);
      c = md5_FF(c, d, a, b, x[k + 6], S13, 0xa8304613);
      b = md5_FF(b, c, d, a, x[k + 7], S14, 0xfd469501);
      a = md5_FF(a, b, c, d, x[k + 8], S11, 0x698098d8);
      d = md5_FF(d, a, b, c, x[k + 9], S12, 0x8b44f7af);
      c = md5_FF(c, d, a, b, x[k + 10], S13, 0xffff5bb1);
      b = md5_FF(b, c, d, a, x[k + 11], S14, 0x895cd7be);
      a = md5_FF(a, b, c, d, x[k + 12], S11, 0x6b901122);
      d = md5_FF(d, a, b, c, x[k + 13], S12, 0xfd987193);
      c = md5_FF(c, d, a, b, x[k + 14], S13, 0xa679438e);
      b = md5_FF(b, c, d, a, x[k + 15], S14, 0x49b40821);
      a = md5_GG(a, b, c, d, x[k + 1], S21, 0xf61e2562);
      d = md5_GG(d, a, b, c, x[k + 6], S22, 0xc040b340);
      c = md5_GG(c, d, a, b, x[k + 11], S23, 0x265e5a51);
      b = md5_GG(b, c, d, a, x[k + 0], S24, 0xe9b6c7aa);
      a = md5_GG(a, b, c, d, x[k + 5], S21, 0xd62f105d);
      d = md5_GG(d, a, b, c, x[k + 10], S22, 0x2441453);
      c = md5_GG(c, d, a, b, x[k + 15], S23, 0xd8a1e681);
      b = md5_GG(b, c, d, a, x[k + 4], S24, 0xe7d3fbc8);
      a = md5_GG(a, b, c, d, x[k + 9], S21, 0x21e1cde6);
      d = md5_GG(d, a, b, c, x[k + 14], S22, 0xc33707d6);
      c = md5_GG(c, d, a, b, x[k + 3], S23, 0xf4d50d87);
      b = md5_GG(b, c, d, a, x[k + 8], S24, 0x455a14ed);
      a = md5_GG(a, b, c, d, x[k + 13], S21, 0xa9e3e905);
      d = md5_GG(d, a, b, c, x[k + 2], S22, 0xfcefa3f8);
      c = md5_GG(c, d, a, b, x[k + 7], S23, 0x676f02d9);
      b = md5_GG(b, c, d, a, x[k + 12], S24, 0x8d2a4c8a);
      a = md5_HH(a, b, c, d, x[k + 5], S31, 0xfffa3942);
      d = md5_HH(d, a, b, c, x[k + 8], S32, 0x8771f681);
      c = md5_HH(c, d, a, b, x[k + 11], S33, 0x6d9d6122);
      b = md5_HH(b, c, d, a, x[k + 14], S34, 0xfde5380c);
      a = md5_HH(a, b, c, d, x[k + 1], S31, 0xa4beea44);
      d = md5_HH(d, a, b, c, x[k + 4], S32, 0x4bdecfa9);
      c = md5_HH(c, d, a, b, x[k + 7], S33, 0xf6bb4b60);
      b = md5_HH(b, c, d, a, x[k + 10], S34, 0xbebfbc70);
      a = md5_HH(a, b, c, d, x[k + 13], S31, 0x289b7ec6);
      d = md5_HH(d, a, b, c, x[k + 0], S32, 0xeaa127fa);
      c = md5_HH(c, d, a, b, x[k + 3], S33, 0xd4ef3085);
      b = md5_HH(b, c, d, a, x[k + 6], S34, 0x4881d05);
      a = md5_HH(a, b, c, d, x[k + 9], S31, 0xd9d4d039);
      d = md5_HH(d, a, b, c, x[k + 12], S32, 0xe6db99e5);
      c = md5_HH(c, d, a, b, x[k + 15], S33, 0x1fa27cf8);
      b = md5_HH(b, c, d, a, x[k + 2], S34, 0xc4ac5665);
      a = md5_II(a, b, c, d, x[k + 0], S41, 0xf4292244);
      d = md5_II(d, a, b, c, x[k + 7], S42, 0x432aff97);
      c = md5_II(c, d, a, b, x[k + 14], S43, 0xab9423a7);
      b = md5_II(b, c, d, a, x[k + 5], S44, 0xfc93a039);
      a = md5_II(a, b, c, d, x[k + 12], S41, 0x655b59c3);
      d = md5_II(d, a, b, c, x[k + 3], S42, 0x8f0ccc92);
      c = md5_II(c, d, a, b, x[k + 10], S43, 0xffeff47d);
      b = md5_II(b, c, d, a, x[k + 1], S44, 0x85845dd1);
      a = md5_II(a, b, c, d, x[k + 8], S41, 0x6fa87e4f);
      d = md5_II(d, a, b, c, x[k + 15], S42, 0xfe2ce6e0);
      c = md5_II(c, d, a, b, x[k + 6], S43, 0xa3014314);
      b = md5_II(b, c, d, a, x[k + 13], S44, 0x4e0811a1);
      a = md5_II(a, b, c, d, x[k + 4], S41, 0xf7537e82);
      d = md5_II(d, a, b, c, x[k + 11], S42, 0xbd3af235);
      c = md5_II(c, d, a, b, x[k + 2], S43, 0x2ad7d2bb);
      b = md5_II(b, c, d, a, x[k + 9], S44, 0xeb86d391);
      a = md5_AddUnsigned(a, AA);
      b = md5_AddUnsigned(b, BB);
      c = md5_AddUnsigned(c, CC);
      d = md5_AddUnsigned(d, DD);
    }
    if (bit == 32) {
      return (md5_WordToHex(a) + md5_WordToHex(b) + md5_WordToHex(c) + md5_WordToHex(d)).toLowerCase();
    }
    return (md5_WordToHex(b) + md5_WordToHex(c)).toLowerCase();
  }
}

export default StringUtil;
