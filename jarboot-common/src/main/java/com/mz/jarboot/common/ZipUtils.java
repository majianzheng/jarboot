package com.mz.jarboot.common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.*;

/**
 * @author majianzheng
 */
public class ZipUtils {
    /** 2M */
    private static final int  BUFFER_SIZE = 1024 * 1024 * 2;

    /**
     * 压缩ZIP
     * @param src 目标目录
     * @param out 输出流
     * @param keepDirStructure 是否保持目录结构
     */
    public static void toZip(File src, OutputStream out, boolean keepDirStructure) {
        try (ZipOutputStream zos = new ZipOutputStream(out)){
            compress(src, zos, src.getName(), keepDirStructure);
        } catch (Exception e) {
            throw new JarbootException("Zip error from ZipUtils", e);
        }
    }

    /**
     * 解压缩ZIP
     * @param zip zip文件
     * @param dest 目标目录
     */
    public static void unZip(File zip, File dest) {
        try (ZipFile zipFile = new ZipFile(zip, StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry element = entries.nextElement();
                String name = element.getName();
                //macOS中的跳过__MACOSX
                if (name.contains("__MACOSX")) {
                    continue;
                }
                File file = new File(dest, name);
                if (element.isDirectory()) {
                    if (!file.mkdirs()) {
                        throw new JarbootException("解压缩文件，创建目录失败！");
                    }
                } else {
                    writeUnZipFile(zipFile, element, file);
                }
            }
        } catch (IOException e) {
            throw new JarbootException(e.getMessage(), e);
        }
    }

    private static void writeUnZipFile(ZipFile zipFile, ZipEntry element, File file) throws IOException {
        File patentDir = file.getParentFile();
        if (!patentDir.exists() && !patentDir.mkdirs()) {
            throw new JarbootException("解压缩文件，创建文件夹失败！");
        }
        if (file.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(file);
                 InputStream in = zipFile.getInputStream(element)) {
                int index = 0;
                byte[] buffer = new byte[BUFFER_SIZE];
                while (-1 != (index = in.read(buffer))) {
                    fos.write(buffer, 0, index);
                }
            }
        } else {
            throw new JarbootException("解压缩文件，创建文件失败！");
        }
    }

    private static void compress(File src, ZipOutputStream zos, String name,
                                 boolean keepDirStructure) throws Exception {
        byte[] buf = new byte[BUFFER_SIZE];
        if(src.isFile()){
            zos.putNextEntry(new ZipEntry(name));
            try (FileInputStream in = new FileInputStream(src)) {
                int len;
                while (-1 != (len = in.read(buf))) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
            }
        } else {
            compressDir(src, zos, name, keepDirStructure);
        }
    }

    private static void compressDir(File src, ZipOutputStream zos, String name, boolean keepDirStructure) throws Exception {
        File[] listFiles = src.listFiles();
        if(null == listFiles || 0 == listFiles.length){
            if(keepDirStructure) {
                zos.putNextEntry(new ZipEntry(name + File.separator));
                zos.closeEntry();
            }
        } else {
            for (File file : listFiles) {
                if (keepDirStructure) {
                    compress(file, zos, name + File.separator + file.getName(), true);
                } else {
                    compress(file, zos, file.getName(), false);
                }
            }
        }
    }

    private ZipUtils() {}
}
