package io.github.majianzheng.jarboot.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.common.JarbootException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Http工具类
 * @author majianzheng
 */
public class HttpUtils {
    /** 连接超时 */
    private static final int CONNECT_TIMEOUT = 30000;

    public static final String CONTENT_TYPE = "content-type";
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final int SUCCESS_STATUS = 200;
    public static final SSLContext SSL_CONTEXT;
    public static final SSLConnectionSocketFactory SSL_CONNECTION_SOCKET_FACTORY;
    private static final CloseableHttpClient HTTP_CLIENT;

    public static JsonNode get(String url, Map<String, String> header) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = doRequest(request, CONTENT_TYPE_FORM, header);
             InputStream is = response.getEntity().getContent()) {
            return JsonUtils.readAsJsonNode(is);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            request.releaseConnection();
        }
    }

    /**
     * Get请求
     * @param url api接口
     * @param type 期望的结果类型
     * @param <T> 范型类
     * @param header 头
     * @return 期望的结构
     */
    public static <T> T getObj(String url, Class<T> type, Map<String, String> header) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = doRequest(request, CONTENT_TYPE_FORM, header);
             InputStream is = response.getEntity().getContent()) {
            return JsonUtils.readValue(is, type);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            request.releaseConnection();
        }
    }

    public static JsonNode postJson(String url, Object json, Map<String, String> header) {
        String content = null == json ? StringUtils.EMPTY : JsonUtils.toJsonString(json);
        return doPost(url, new StringEntity(content, StandardCharsets.UTF_8), CONTENT_TYPE_JSON, header);
    }

    /**
     * post请求
     * @param url api接口
     * @param data 请求参数
     * @param type 范型类
     * @return 期望的结果类型
     * @param <T>
     */
    public static <T> T  postObjByString(String url, String data, Class<T> type, Map<String, String> header) {
        return doPost(url, new StringEntity(data, StandardCharsets.UTF_8), CONTENT_TYPE_JSON, header, type);
    }

    /**
     * Post请求
     * @param url api接口
     * @param object 传入的参数
     * @param type 期望的结果类型
     * @param header 头
     * @param <T> 范型类
     * @return 期望的结构
     */
    public static <T> T postObj(String url, Object object, Class<T> type, Map<String, String> header) {
        String content = null == object ? StringUtils.EMPTY : JsonUtils.toJsonString(object);
        return doPost(url, new StringEntity(content, StandardCharsets.UTF_8), CONTENT_TYPE_JSON, header, type);
    }

    public static JsonNode post(String url, Map<String, String> formData, Map<String, String> header) {
        HttpEntity request = convertFormEntity(formData);
        return doPost(url, request, CONTENT_TYPE_FORM, header);
    }

    public static JsonNode delete(String url, Map<String, String> header) {
        HttpDelete request = new HttpDelete(url);
        try (CloseableHttpResponse response = doRequest(request, CONTENT_TYPE_FORM, header);
             InputStream is = response.getEntity().getContent()) {
            return JsonUtils.readAsJsonNode(is);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            request.releaseConnection();
        }
    }

    public static JsonNode doPost(String url, HttpEntity request, String contentType, Map<String, String> header) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(request);
        try (CloseableHttpResponse response = doRequest(httpPost, contentType, header);
             InputStream is = response.getEntity().getContent()) {
            return JsonUtils.readAsJsonNode(is);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    public static <T> T  doPost(String url, HttpEntity request, String contentType, Map<String, String> header, Class<T> cls) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(request);
        try (CloseableHttpResponse response = doRequest(httpPost, contentType, header);
             InputStream is = response.getEntity().getContent()) {
            return JsonUtils.readValue(is, cls);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    public static void get(String url, OutputStream os, Map<String, String> header) {
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = doRequest(httpGet, null, header);
             InputStream is = response.getEntity().getContent()) {
            IOUtils.copy(is, os);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            httpGet.releaseConnection();
        }
    }

    public static String getString(String url, Map<String, String> header) {
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpResponse response = doRequest(httpGet, null, header);
             InputStream is = response.getEntity().getContent()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            httpGet.releaseConnection();
        }
    }

    public static void post(String url, OutputStream os, Map<String, String> param, Map<String, String> header) {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity request = convertFormEntity(param);
        httpPost.setEntity(request);
        try (CloseableHttpResponse response = doRequest(httpPost, null, header);
             InputStream is = response.getEntity().getContent()) {
            IOUtils.copy(is, os);
        } catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    public static void upload(String url, InputStream is, String filename, Map<String, String> param, Map<String, String> header) {
        // 把一个普通参数和文件上传给下面这个地址 是一个servlet
        HttpPost httpPost = new HttpPost(url);
        fillHeader(httpPost, header);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                // 相当于<input type="file" name="file"/>
                .addPart("file", new InputStreamBody(is, filename));
        if (null != param) {
            param.forEach(builder::addTextBody);
        }
        httpPost.setEntity(builder.build());
        // 发起请求 并返回请求的响应
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(httpPost)) {
            // 获取响应对象
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                // 销毁
                EntityUtils.consume(resEntity);
            }
        }  catch (Exception e) {
            throw new JarbootException(e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    private static CloseableHttpResponse doRequest(HttpRequestBase request, String contentType, Map<String, String> header) throws IOException {
        fillHeader(request, header);
        if (StringUtils.isNotEmpty(contentType)) {
            request.setHeader(CONTENT_TYPE, contentType);
        }
        CloseableHttpResponse response = HTTP_CLIENT.execute(request);
        checkStatus(response);
        return response;
    }

    private static HttpEntity convertFormEntity(Map<String, String> formData) {
        HttpEntity request;
        if (null == formData || formData.isEmpty()) {
            request = new StringEntity(StringUtils.EMPTY, StandardCharsets.UTF_8);
        } else {
            List<NameValuePair> nameValuePairList = formData
                    .entrySet()
                    .stream()
                    .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            try {
                request = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
            } catch (Exception e) {
                request = new StringEntity(StringUtils.EMPTY, StandardCharsets.UTF_8);
            }
        }
        return request;
    }

    private static void fillHeader(HttpRequestBase httpMessage, Map<String, String> header) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(CONNECT_TIMEOUT)
                .build();
        httpMessage.setConfig(requestConfig);
        if (null != header && !header.isEmpty()) {
            header.forEach(httpMessage::addHeader);
        }
    }
    private static void checkStatus(CloseableHttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        if (SUCCESS_STATUS != statusCode) {
            throw new JarbootException(statusCode, "请求失败，status code:" + statusCode);
        }
    }
    static  {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(new TrustSelfSignedStrategy())
                    .build();
        } catch (Exception e) {
            throw new JarbootException(e);
        }

        // we can optionally disable hostname verification.
        // if you don't want to further weaken the security, you don't have to include this.
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

        // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
        // and allow all hosts verifier.
        SSL_CONNECTION_SOCKET_FACTORY = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
        SSL_CONTEXT = sslContext;
        HTTP_CLIENT = HttpClients.custom().setSSLSocketFactory(SSL_CONNECTION_SOCKET_FACTORY).build();
    }

    private HttpUtils() {}
}
