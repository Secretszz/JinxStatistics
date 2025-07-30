package com.jinx.statistics.utility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtility {

    public static File getFile(String path) throws Exception {
        File file = new File(path);
        createFile(file);
        return file;
    }

    public static File getDirectory(String path) {
        File file = new File(path);
        createFolder(file);
        return file;
    }

    public static boolean createFile(File file) throws Exception {
        if(!file.exists()){
            createFolder(file.getParentFile());
            return file.createNewFile();
        }
        return true;
    }

    public static boolean createFolder(File file){
        if(!file.exists()){
            return file.mkdirs();
        }
        return true;
    }

    public static File openFile(String path) {
        return new File(path);
    }

    public static void writeFully(File file, byte[] content, boolean append) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file, append)) {
            output.write(content);
            output.flush();
        }
    }

    public static void writeFully(File file, String content, boolean append) throws IOException {
        writeFully(file, content.getBytes(StandardCharsets.UTF_8), append);
    }

    /**
     * 压缩文件夹
     * @param sourceFolder 源文件夹路径
     * @param zipFilePath 压缩文件保存路径
     * @throws IOException 当发生输入输出异常时抛出
     */
    public static void zipFolder(String sourceFolder, String zipFilePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {
            File fileToZip = new File(sourceFolder);
            zipFile(fileToZip, fileToZip.getName(), zipOut);
        }
    }

    /**
     * 递归压缩文件
     * @param fileToZip 要压缩的文件或文件夹
     * @param fileName 文件名
     * @param zipOut 压缩输出流
     * @throws IOException 当发生输入输出异常时抛出
     */
    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }
}
