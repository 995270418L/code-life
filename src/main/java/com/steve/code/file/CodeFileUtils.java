package com.steve.code.file;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class CodeFileUtils {

  /**
   * 获取文件的完整名字， 当前端上传文件时不带后缀就需要用这个方法获取完整的文件名
   * @param name 不带后缀的名字
   * @param originName 文件的原始名，用于获取文件的后缀
   * @return
   */
  public String getFullName(String name, String originName){
    String[] split = name.split("\\.");
    if(split.length > 1){
      return specialNameReplace(name);
    }
    if(split.length <= 1 && originName == null) return specialNameReplace(name);
    return specialNameReplace(name + originName.substring(originName.lastIndexOf(".") == -1 ? 0 : originName.lastIndexOf(".")));
  }

  public String specialNameReplace(String name){
    return name.replaceAll("[\\\\/:*?\"<>'|]", "_");
  }

  /**
   * rename 的概念是在文件名的后面 append 一段时间戳
   * @param name 文件/文件夹 名字
   * @param isFolder 如果是文件，需要在 后缀名前加上 时间戳，文件夹则直接 append 即可
   * @return
   */
  public String rename(String name, boolean isFolder){
    Long suffix = System.currentTimeMillis();
    String res = null;
    if(isFolder){
      int idx = name.lastIndexOf("_");
      if(isRename(name)){
        res = name.substring(0, idx + 1) + suffix;
      } else {
        res = name + "_" + suffix;
      }
    }else{
      int pointIdx = name.lastIndexOf(".");
      int deshIdx = name.lastIndexOf("_");
      pointIdx = pointIdx == -1 ? name.length() : pointIdx;
      if(isRename(name)){
        res = name.substring(0, deshIdx) + "_" + suffix + name.substring(pointIdx);
      }else{
        res = name.substring(0, pointIdx) + "_" + suffix + name.substring(pointIdx);
      }
    }
    return specialNameReplace(res);
  }

  public boolean isRename(String name){
    int idx = name.lastIndexOf("_");
    if(idx != -1){
      int pointIdx = name.lastIndexOf(".");
      String numStr = null;
      if(pointIdx != -1) {
        numStr = name.substring(idx+1, pointIdx);
      }else{
        numStr = name.substring(idx+1);
      }
      try {
        long num = Long.valueOf(numStr);
        new Date(num);
        return true;
      }catch (Exception e){
        return false;
      }
    }
    return false;
  }

  public String getFileType(String name){
    String[] nameSplit = name.split("\\.");
    if(nameSplit.length > 1){
      return nameSplit[nameSplit.length - 1];
    }else{
      log.warn("该文件名没有后缀");
      return null;
    }
  }

}
