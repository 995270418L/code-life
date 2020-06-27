package com.steve.code.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CodeFileHashUtils {

  public static String md5Hash(Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    int blockBytes = 4 * 1024 * 1024;
    try(FileInputStream inputStream = new FileInputStream(path.toFile())){
      byte[] buffer = new byte[blockBytes];
      int length = -1;
      while ((length = inputStream.read(buffer)) != -1) md5.update(buffer, 0, length);
    }
    return toHex(md5.digest());
  }

  public static String md5Hash(byte[] bytes) throws IOException, NoSuchAlgorithmException {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    return toHex(md5.digest(bytes));
  }

  public static String sha256Hash(byte[] bytes) throws IOException, NoSuchAlgorithmException {
    MessageDigest md5 = MessageDigest.getInstance("SHA-256");
    return toHex(md5.digest(bytes));
  }

  public static String sha256Hash(Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    int blockBytes = 4 * 1024 * 1024;
    try(FileInputStream inputStream = new FileInputStream(path.toFile())){
      byte[] buffer = new byte[blockBytes];
      int length = -1;
      while ((length = inputStream.read(buffer)) != -1) sha256.update(buffer, 0, length);
    }
    return toHex(sha256.digest());
  }

  public static String toHex(byte[] digest) {
    final char[] Hex = "0123456789ABCDEF".toCharArray();
    StringBuilder ret = new StringBuilder(digest.length * 2);
    for (byte v : digest) {
      ret.append(Hex[(v >> 4) & 0x0f]);
      ret.append(Hex[v & 0x0f]);
    }
    return ret.toString();
  }

}
