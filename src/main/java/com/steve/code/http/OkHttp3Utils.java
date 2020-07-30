package com.steve.code.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.http.ContentDisposition;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttp3Utils {

  private static OkHttpClient okHttpClient = null;

  static {
    okHttpClient = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)  // 开启出错重试, 默认 4 次
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
  }

  /**
   * 根据文件名生成 content type 未识别的后缀返回 application/octet-stream
   * @param filename
   * @return
   */
  public static String contentTypeHeader(String filename){
    if(filename == null) return null;
    MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
    return fileTypeMap.getContentType(filename);
  }

  /**
   * 根据文件名生成 disposition header value
   * @param filename
   * @return
   */
  public static String dispositionHeader(String filename){
    if(filename == null) return null;
    ContentDisposition attachment = ContentDisposition.builder("attachment").filename(filename, Charset.forName("UTF-8")).build();
    return attachment.toString();
  }


  public static String get(String url, Headers headers) {
    Request request = new Request.Builder()
            .url(url)
            .headers(headers)
            .get()
            .build();
    return dealResponse(request);
  }

  private static String dealResponse(Request request) {
    Call call = okHttpClient.newCall(request);
    Response response = null;
    try {
      response = call.execute();
      if (!response.isSuccessful()) {
        log.error("服务器请求失败： {}", request.url());
        return null;
      }
      String result = response.body().string();
      log.info("请求结果：{}\t 请求参数：{}", result, request.body());
      return result;
    } catch (IOException e) {
      log.error("请求出错, 请求地址: {}", request.url());
      e.printStackTrace();
    } finally {
      if (response != null) {
        response.body().close();
        response.close();
      }
    }
    return null;
  }

  private static Response headResponse(Request request) {
    Call call = okHttpClient.newCall(request);
    Response response = null;
    try {
      response = call.execute();
      if (!response.isSuccessful()) {
        log.error("服务器请求失败： {}", request.url());
        return null;
      }
      return response;
    } catch (IOException e) {
      log.error("请求出错, 请求地址: {}", request.url());
      e.printStackTrace();
    } finally {
      if (response != null) {
        response.body().close();
        response.close();
      }
    }
    return null;
  }

  public static String doPost(String url, Map<String, String> formData, MediaType mediaType, Headers headers, Path path){
    MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
    if(path != null){
      RequestBody body = RequestBody.create(mediaType == null ? MediaType.parse("application/octet-stream; charset=utf-8") : mediaType, path.toFile());
      requestBody.addFormDataPart("file", path.toFile().getName(), body);
    }
    if (formData != null) {
      // map 里面是请求中所需要的 key 和 value
      for (Map.Entry<String, String> entry : formData.entrySet()) {
        requestBody.addFormDataPart(entry.getKey(), entry.getValue());
      }
    }
    Request request = new Request.Builder().url(url).post(requestBody.build()).headers(headers).build();
    return dealResponse(request);
  }
  public static String doPut(String url, byte[] data, Headers headers, MediaType mediaType){
    RequestBody requestBody = RequestBody.create(data, mediaType == null ? MediaType.parse("application/octet-stream; charset=utf-8") : mediaType);
    Request request = new Request.Builder().url(url).put(requestBody).headers(headers).build();
    return dealResponse(request);
  }

  public static String doGet(String url, Headers headers){
    // 暂不支持
    return null;
  }

  public static Headers doHead(String url){
    Request request = new Request.Builder().url(url).head().build();
    Response response =  headResponse(request);
    return response.headers();
  }

}
