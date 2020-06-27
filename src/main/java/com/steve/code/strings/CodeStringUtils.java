package com.steve.code.strings;

import com.steve.code.file.CodeFileHashUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class CodeStringUtils {

  public static String signature(String key, String st, String se){
    String signatureKey = key + "#" + st + "#" + se;
    try {
      return CodeFileHashUtils.md5Hash(signatureKey.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

}