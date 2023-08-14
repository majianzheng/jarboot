/**
 * http错误处理公用类
 * @author majianzheng
 */
export default class ErrorUtil {
  private static codeMessage: any = {
    200: { 'zh-CN': '请求成功！', 'en-US': 'Request success!' },
    201: { 'zh-CN': '已创建。成功请求并创建了新的资源', 'en-US': 'Created. A new resource was successfully requested and created' },
    202: { 'zh-CN': '已接受。已经接受请求，但未处理完成', 'en-US': 'Accepted. The request was accepted but not processed' },
    204: {
      'zh-CN': '无内容。服务器成功处理，但未返回内容',
      'en-US': 'No content. The server processed successfully, but did not return content',
    },
    400: { 'zh-CN': '客户端请求的语法错误，服务器无法解析', 'en-US': "Syntax error of client request, the server can't understand it" },
    401: { 'zh-CN': '用户未登录认证或已过期', 'en-US': 'The user is not logged in or the authentication has expired' },
    403: { 'zh-CN': '权限验证失败,请检查权限', 'en-US': 'Permission verification failed, please check the permission' },
    404: {
      'zh-CN': '发出的请求针对的是不存在的记录，服务器没有进行操作',
      'en-US': 'The request is for a record that does not exist, and the server does not operate',
    },
    406: { 'zh-CN': '服务器无法根据客户端请求的内容特性完成请求', 'en-US': 'Not Acceptable' },
    410: { 'zh-CN': '客户端请求的资源已经不存在', 'en-US': 'The resource requested by the client no longer exists' },
    500: { 'zh-CN': '服务器内部错误，无法完成请求', 'en-US': 'Internal Server Error' },
    502: { 'zh-CN': '网关错误', 'en-US': 'Bad Gateway' },
    503: { 'zh-CN': '由于超载或系统维护，服务器暂时的无法处理客户端的请求', 'en-US': 'Service Unavailable' },
    504: { 'zh-CN': '网关超时', 'en-US': 'Gateway Time-out' },
  };
}
