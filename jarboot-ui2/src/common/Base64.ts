/**
 * Base64
 */
export class Base64 {
  public static base64UrlEncode(str: string): string {
    const encoder = new TextEncoder();
    const data = encoder.encode(str);

    // 更安全的大数据处理方式
    let binaryString = '';
    for (let i = 0; i < data.length; i++) {
      binaryString += String.fromCharCode(data[i]);
    }

    let base64 = btoa(binaryString);

    // 精确处理尾随等号 (0-2个)
    const paddingIndex = base64.indexOf('=');
    if (paddingIndex !== -1) {
      base64 = base64.substring(0, paddingIndex);
    }

    return base64.replace(/\+/g, '-').replace(/\//g, '_');
  }

  public static encodeUnicodeToBase64(str: string) {
    const encoder = new TextEncoder();
    const byteArray = encoder.encode(str);
    return btoa(String.fromCharCode(...byteArray));
  }

  public static decodeBase64ToUnicode(base64Str: string) {
    const binaryString = atob(base64Str);
    const byteArray = Uint8Array.from(binaryString, c => c.charCodeAt(0));
    const decoder = new TextDecoder('utf-8');
    return decoder.decode(byteArray);
  }
}
