package com.steve.code.file;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CodePathUtils {

  /**
   * 去除不同分割符路径 对结果的影响
   * @param path
   * @return
   */
  public static List<String> pathFilterList(String path){
    String[] split = pathArray(path);
    List<String> pathList = new LinkedList<String>();
    for(String str: split){
      if(str.equals("") || str.trim().equals("")){
        continue;
      }
      pathList.add(str);
    }
    return pathList;
  }

  /**
   * 划分路径， 可以实现 不同操作系统路径 格式的划分
   * @param path "/steve/jobs" | "\\steve/jobs"
   * @return
   */
  public static String[] pathArray(String path){
    Assert.notNull(path, "路径不能为空");
    return path.split("[/\\\\]");
  }

}
