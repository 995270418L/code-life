package com.steve.code.file;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
//import org.springframework.boot.system.ApplicationHome;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import static org.springframework.util.StreamUtils.BUFFER_SIZE;

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


  /**
   * 把整个文件夹移动到另一个文件夹里面
   * @param source
   * @param target
   * @throws IOException
   */
  public static void copyDir(Path source, Path target) throws IOException {
    copyDir(source, target, StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * 把整个文件夹移动到另一个文件夹里面
   * @param source
   * @param target
   * @param options
   * @throws IOException
   */
  public static void copyDir(Path source, Path target, CopyOption... options) throws IOException {
    operatorDir(false, source, target, options);
  }

  /**
   * 移动 src 文件夹下的文件和文件夹到 tar 文件夹下面
   * @param source
   * @param target
   */
  public static void copyDirFiles(Path source, Path target) throws IOException {
    Set<FileVisitOption> visitOptionSet =  new HashSet(){{add(FileVisitOption.FOLLOW_LINKS);}};
    Files.walkFileTree(source, visitOptionSet, 1, new SimpleFileVisitor<Path>(){

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(!Files.isDirectory(file)){
          Files.copy(file, target.resolve(file.subpath(source.getNameCount(), file.getNameCount())), StandardCopyOption.REPLACE_EXISTING);
        }else{
          copyDir(file, target);
        }
        return super.visitFile(file, attrs);
      }
    });
  }

  public static void operatorDir(boolean move, Path source, Path target, CopyOption... options) throws IOException {
    if(null==source||!Files.isDirectory(source))
      throw new IllegalArgumentException("source must be directory");
    Path dest = target.resolve(source.getFileName());
    // 如果相同则返回
    if(Files.exists(dest)&&Files.isSameFile(source, dest))return;
    // 目标文件夹不能是源文件夹的子文件夹
    if(isSub(source, dest))
      throw new IllegalArgumentException("dest must not  be sub directory of source");
    boolean clear=true;
    for(CopyOption option:options)
      if(StandardCopyOption.REPLACE_EXISTING==option){
        clear=false;
        break;
      }
    // 如果指定了REPLACE_EXISTING选项则不清除目标文件夹
    if(clear)
      deleteIfExists(dest);
    Files.walkFileTree(source,  new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        // 在目标文件夹中创建dir对应的子文件夹
        Path subDir = 0 ==dir.compareTo(source) ? dest : dest.resolve(dir.subpath(source.getNameCount(), dir.getNameCount()));
        Files.createDirectories(subDir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(move)
          Files.move(file, dest.resolve(file.subpath(source.getNameCount(), file.getNameCount())), options);
        else
          Files.copy(file, dest.resolve(file.subpath(source.getNameCount(), file.getNameCount())), options);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        // 移动操作时删除源文件夹
        if(move)
          Files.delete(dir);
        return super.postVisitDirectory(dir, exc);
      }
    });
  }

  public static boolean isSub(Path src, Path dest) throws IOException {
    if( null == src || null == dest){
      throw new NullPointerException("src and dest can't be null");
    }
    while(null != dest){
      if(Files.exists(dest) && Files.isSameFile(src, dest)) return true;
      dest = dest.getParent();
    }
    return false;
  }

  public static void deleteIfExists(Path dir) throws IOException {
    try {
      Files.deleteIfExists(dir);
    } catch (DirectoryNotEmptyException e) {
      Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return super.postVisitDirectory(dir, exc);
        }
      });
    }
  }

  public static void regexChangeFile(String srcDir, String srcFilename, String tarDir, String tarFilename, Map<String, String> contentMap) {
    InputStream inputStream = null;
    FileWriter fileWriter = null;
    try {
      inputStream = new FileInputStream(srcDir + srcFilename);
      File file = new File(tarDir);
      if (!file.exists()) {
        file.mkdirs();
      }
      fileWriter = new FileWriter(tarDir + "/" + tarFilename);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        String prefix = line.split("=")[0];
        if (contentMap.keySet().contains(prefix)) {
          line = prefix + "=" + contentMap.get(prefix);
        }
        fileWriter.write(line + System.lineSeparator());
      }
      fileWriter.flush();
      bufferedReader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        inputStream.close();
        fileWriter.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 从zip文件中读取指定文件内容
   * @param zipFile zip 格式的文件
   * @param filename zip内部文件
   * @return
   */
  public static String readZipFileWithoutDecompress(File zipFile, String filename){
    StringBuilder sb = null;
    ZipFile file = null;
    try {
      sb = new StringBuilder();
      file = new ZipFile(zipFile);
      Enumeration<? extends ZipEntry> zipFileEntries = file.entries();
      while (zipFileEntries.hasMoreElements()) {
        ZipEntry zipEntry = zipFileEntries.nextElement();
        if (zipEntry.getName().endsWith(filename)) {
          try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream(zipEntry)))){
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
              sb.append(line).append(System.lineSeparator());
            }
          }
          break;
        }
      }
    } catch (ZipException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }finally {
      try {
        file.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }

  /**
   * 从 tar 文件中读取指定文件内容
   * @param tarFile
   * @param fileName
   * @return
   */
  public static String readTarFileWithoutDecompress(File tarFile, String fileName){
    String res = null;
    try(TarArchiveInputStream tarIn = new TarArchiveInputStream(
            new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarFile))))){
      TarArchiveEntry entry = null;
      while ((entry = tarIn.getNextTarEntry()) != null) {
        if(entry.getName().endsWith(fileName)){
          try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int length;
            byte[] b = new byte[BUFFER_SIZE];
            while ((length = tarIn.read(b)) != -1) {
              out.write(b, 0, length);
            }
            res = out.toString("utf-8");
          }
          break;
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return res;
  }

  public static String readFile(File srcFile) {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile)))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append(System.lineSeparator());
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  public static void createFloder(String systemDefault) {
    String dmFloder = System.getenv("DATAMESH_HOME");
    File file;
    if (dmFloder != null) {
      file = new File(dmFloder);
    } else {
      file = new File(systemDefault);
    }
    if (!file.exists()) {
      if (!file.mkdirs()) {
        log.error("mkdir directory failed, target directory: {}", file.getName());
      }
    }
  }

  public static void writeContentToFile(String content, Path tarFile) throws IOException {
    if(Files.exists(tarFile)){
      Files.write(tarFile, content.getBytes(), StandardOpenOption.WRITE);
    }else{
      Files.write(tarFile, content.getBytes(), StandardOpenOption.CREATE);
    }
  }

  public static void writeFile(File srcFile, File targetFile) {
    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;
    try {
      inputStream = new FileInputStream(srcFile);
      outputStream = new FileOutputStream(targetFile);
      int byteCount = 0, byteWritten = 0;
      byte[] bytes = new byte[1024];
      while ((byteCount = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, byteWritten, byteCount);
        byteWritten += byteCount;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void saveToFile(String content, File target){
    FileOutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(target);
      outputStream.write(content.getBytes());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }finally {
      close(outputStream);
    }
  }

  public static void transformFile(File source, File target) {
    FileChannel in = null;
    FileChannel out = null;
    FileInputStream inStream = null;
    FileOutputStream outStream = null;
    try {
      inStream = new FileInputStream(source);
      outStream = new FileOutputStream(target);
      in = inStream.getChannel();
      out = outStream.getChannel();
      in.transferTo(0, in.size(), out);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      close(inStream);
      close(in);
      close(outStream);
      close(out);
    }
  }

  private static void close(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 获取 jar 包文件的路径
   * @return
   */
//  public static Path jarPath(Class clazz){
//    ApplicationHome h = new ApplicationHome(clazz);
//    File jarFile = h.getSource();
//    String path = jarFile.toString();
//    if(path.endsWith("jar")){
//      path = path.substring(0, path.lastIndexOf(File.separator) + 1);
//      log.info("path 以 jar 结尾, remove result: {}", path);
//    }
//    log.info("jar 包文件的绝对路径: {}", path);
//    return Paths.get(path, folderName);
//  }


}
