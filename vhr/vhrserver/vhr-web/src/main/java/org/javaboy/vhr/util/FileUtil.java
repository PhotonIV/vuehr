package org.javaboy.vhr.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author image
 * @Date 2019/3/29 17:34
 **/

@Slf4j
public class FileUtil {

    public final static String ENCODE = "UTF-8";
    private static List<String> fileName;

    public static File getFilePath(String filePath) throws FileNotFoundException {
        if (filePath==null||filePath=="") {
            throw new FileNotFoundException("文件路径为空！");
        }
        return new File(filePath);
    }

    /**
     * 判断目录（文件）是否存在
     *
     * @param filePath 目录（文件）地址
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: boolean
     */
    public static boolean existFile(String filePath) throws FileNotFoundException {
        File file = getFilePath(filePath);
        return file.exists();
    }


    /**
     * 删除目录（文件）
     *
     * @param filePath 目录（文件）地址
     * @param isDel    是否删除子目录
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
    public static void deleteFiles(String filePath, boolean isDel) throws FileNotFoundException {
        File file = getFilePath(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filenames = file.list();
                for (String f : filenames) {
                    deleteFiles(getLinkPath(filePath, f), true);
                }
                if (isDel) {
                    file.delete();
                }
            }
        }
        ;
    }

    /**
     * 创建目录，判断是否存在，不存在创建，并授权所有人读写执行权限
     *
     * @param filePath 目录地址
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */

    public static void createFiles(String filePath) throws FileNotFoundException {
        File file = getFilePath(filePath);
        if (!file.exists() ) {
            file.mkdirs();
            file.setReadable(true, false);
            file.setExecutable(true, false);
            file.setWritable(true, false);
        }
    }

    /**
     * 创建上级文件夹，判断是否存在，不存在创建
     *
     * @param filePath
     * @return
     */
    public static void creatParentFiles(String filePath) throws FileNotFoundException {
        File file = getFilePath(filePath);
        createFiles(file.getParentFile().getPath());
    }

    /**
     * 获取文件,如果不是文件直接返回空对象
     *
     * @param filePath 文件地址
     * @return byte[] 文件二进制流
     */
    public static byte[] getFile(String filePath) {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        byte[] result = null;
        try {
            File file = getFilePath(filePath);
            if (!file.exists() || !file.isFile()) {
                return null;
            }
            fis = new FileInputStream(file);
            Long fileLong = file.length();
            bos = new ByteArrayOutputStream(fileLong.intValue());
            byte[] b = new byte[256];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            result = bos.toByteArray();
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
        return result;
    }

    /**
     * 保存文件，如果文件存在，则删除文件，再进行保存
     *
     * @param filePath 文件目录
     * @param bfile    文件二进制流
     * @return void
     */
    public static void saveFile(String filePath, byte[] bfile) {

        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            File file = getFilePath(filePath);
            if (file.exists()) {
                deleteQuietly(file);
            }
            touch(file);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
    public static void touch(File file) throws IOException {
        if (!file.exists()) {
            openOutputStream(file).close();
        }

        boolean success = file.setLastModified(System.currentTimeMillis());
        if (!success) {
            throw new IOException("Unable to set the last modification time for " + file);
        }
    }
    public static FileOutputStream openOutputStream(File file) throws IOException {
        return openOutputStream(file, false);
    }
    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }

            if (!file.canWrite()) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Directory '" + parent + "' could not be created");
            }
        }

        return new FileOutputStream(file, append);
    }
    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        } else {
            try {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                }
            } catch (Exception var3) {
            }

            try {
                return file.delete();
            } catch (Exception var2) {
                return false;
            }
        }
    }
    public static void cleanDirectory(File directory) throws IOException {
        File[] files = verifiedListFiles(directory);
        IOException exception = null;
        File[] var3 = files;
        int var4 = files.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            File file = var3[var5];

            try {
                forceDelete(file);
            } catch (IOException var8) {
                exception = var8;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }
    private static File[] verifiedListFiles(File directory) throws IOException {
        String message;
        if (!directory.exists()) {
            message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        } else if (!directory.isDirectory()) {
            message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        } else {
            File[] files = directory.listFiles();
            if (files == null) {
                throw new IOException("Failed to list contents of " + directory);
            } else {
                return files;
            }
        }
    }
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }

                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }

    }
    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            if (!isSymlink(directory)) {
                cleanDirectory(directory);
            }

            if (!directory.delete()) {
                String message = "Unable to delete directory " + directory + ".";
                throw new IOException(message);
            }
        }
    }
    public static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        } else {
            return Files.isSymbolicLink(file.toPath());
        }
    }
    /**
     * 保存文件，如果文件存在，则删除文件，再进行保存
     *
     * @param filePath 目录地址
     * @param fileName 文件名
     * @param bfile    文件二进制流
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
    public static void saveFile(String filePath, String fileName, byte[] bfile) throws FileNotFoundException {
        String pathFile = getPathFiles(filePath, fileName);
        saveFile(pathFile, bfile);
    }

    /**
     * 获取文件地址，（如果不存在地址，会创建文件夹）
     *
     * @param pathFile 目录地址
     * @param fileName 文件名
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.lang.String 文件地址
     */
    public static String getPathFiles(String pathFile, String fileName) throws FileNotFoundException {
        return getPathFiles(pathFile, fileName, true);
    }

    /**
     * 拼接文件地址
     *
     * @param pathFile 目录地址
     * @param fileName 文件名
     * @param isCreat  是否存在目录
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.lang.String
     */
    public static String getPathFiles(String pathFile, String fileName, boolean isCreat) throws FileNotFoundException {
        if (!(pathFile==null&&pathFile=="")) {
            if (isCreat) {
                FileUtil.createFiles(pathFile);
            }
            if (pathFile.endsWith(File.separator)) {
                return pathFile + fileName;
            } else {
                return pathFile + File.separator + fileName;
            }
        }
        return fileName;
    }

    /**
     * 拼接目录地址
     *
     * @param pathLeft  目录左边
     * @param pathRight 目录右边
     * @param isCreat   是否创建目录
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.lang.String 目录全路径
     */

    public static String getLinkPath(String pathLeft, String pathRight, boolean isCreat) throws FileNotFoundException {
        String pathFile = getLinkPath(pathLeft, pathRight);
        //创建左边目录
        if (isCreat && (!(pathLeft==null&&pathLeft==""))) {
            FileUtil.createFiles(pathLeft);
        }

        //创建全目录
        if (isCreat && (!(pathLeft==null&&pathLeft==""))) {
            FileUtil.createFiles(pathFile);
        }
        return pathFile;

    }

    /**
     * 拼接目录（文件）地址
     *
     * @param pathLeft  目录（文件）左边
     * @param pathRight 目录（文件）右边
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.lang.String 目录（文件）全路径
     */

    public static String getLinkPath(String pathLeft, String pathRight) {
        if (!!(pathLeft==null&&pathLeft=="") && !!(pathRight==null&&pathRight=="")) {
            if ((pathLeft.endsWith(File.separator) && (!pathRight.startsWith(File.separator))) ||
                    ((!pathLeft.endsWith(File.separator)) && (pathRight.startsWith(File.separator)))) {
                return pathLeft + pathRight;
            } else {
                return pathLeft + File.separator + pathRight;
            }
        }
        return pathLeft + pathRight;
    }

    /**
     * 复制目录（文件）
     *
     * @param oldFilePath 原目录（文件）
     * @param newFilePath 新目录（文件）
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
//    public static void copyFile(String oldFilePath, String newFilePath) {
//        try {
//            log.debug(oldFilePath);
//            log.debug(newFilePath);
//            File oldFile = getFilePath(oldFilePath);
//            File newFile = getFilePath(newFilePath);
//            ch.qos.logback.core.util.FileUtil.copyFile(oldFile, newFile);
//        } catch (IOException e) {
//            log.error("复制文件异常", e);
//            throw new AppException("", "参数配置异常，请与管理员联系！", e);
//        }
//    }

    /**
     * 获取绝对路径。
     * 如果为/test,则为根目录下的test
     * 如果为test,则为运行java目录下的test
     *
     * @param filePath 目录（文件）相对路径
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.lang.String 目录（文件）绝对路径
     */
    public static String getCanonicalPath(String filePath) throws FileNotFoundException {
        File file = getFilePath(filePath);
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            log.info("文件路径获取异常：", e);
        }
        return filePath;
    }

    /**
     * 运行修改目录（文件）可执行权限,所有人都有该权限
     *
     * @param filePath 目录（文件）地址
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
//    public static void setFilesExecutable(String filePath) {
//        File files = getFilePath(filePath);
//        if (files.isDirectory()) {
//            List<File> fileList = (List<File>) FileUtils.listFiles(files, null, true);
//            if (fileList != null && fileList.size() > 0) {
//                for (File file : fileList) {
//                    file.setExecutable(true, false);
//                    log.debug("is execute allow : " + files.canExecute());
//                }
//            }
//        } else {
//            files.setExecutable(true, false);
//            log.debug("is execute allow : " + files.canExecute());
//        }
//    }

    /**
     * 运行修改目录（文件）只读权限,所有人都有该权限
     *
     * @param filePath 目录（文件）地址
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
//    public static void setFilesReadable(String filePath) {
//        File files = getFilePath(filePath);
//        if (files.isDirectory()) {
//            List<File> fileList = (List<File>) FileUtils.listFiles(files, null, true);
//            if (fileList != null && fileList.size() > 0) {
//                for (File file : fileList) {
//                    file.setReadable(true, false);
//                    log.debug("is execute allow : " + files.canExecute());
//                }
//            }
//        } else {
//            files.setReadable(true, false);
//            log.debug("is readable allow : " + files.canExecute());
//        }
//    }
//

    /**
     * 运行修改目录（文件）只读写权限,所有人都有该权限
     *
     * @param filePath 目录（文件）地址
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
//    public static void setFilesWritable(String filePath) {
//        File files = getFilePath(filePath);
//        if (files.isDirectory()) {
//            List<File> fileList = (List<File>) FileUtils.listFiles(files, null, true);
//            if (fileList != null && fileList.size() > 0) {
//                for (File file : fileList) {
//                    file.setWritable(true, false);
//                    log.debug("is execute allow : " + files.canExecute());
//                }
//            }
//        } else {
//            files.setWritable(true, false);
//            log.debug("is execute allow : " + files.canExecute());
//        }
//    }


    /**
     * 修改目录（文件）的所有者权限
     *
     * @param filePath 目录地址
     * @param owner    所有人
     * @param group    所有组
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: void
     */
//    public static void setOwner(String filePath, String owner, String group) {
//        try {
//            File file_shellrum = getFilePath(filePath);
//            Path path = file_shellrum.toPath();
////            if (file_shellrum.isFile()) {
////                path = file_shellrum.getParentFile().toPath();
////            }
////            Paths.get(filePath);
//            FileAttributeView fileAttributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
//            if (!HaiyiApiUtil.isNull(group)) {
//                GroupPrincipal groupPrincipal = path.getFileSystem().
//                        getUserPrincipalLookupService().lookupPrincipalByGroupName(group);
//                ((PosixFileAttributeView) fileAttributeView).setGroup(groupPrincipal);
//            }
//            if (!HaiyiApiUtil.isNull(owner)) {
//                UserPrincipal userPrincipal = path.getFileSystem().
//                        getUserPrincipalLookupService().lookupPrincipalByName(owner);
//                ((PosixFileAttributeView) fileAttributeView).setOwner(userPrincipal);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//

    /**
     * 获取文件，非文件返回空对象
     *
     * @param filePath 文件直一
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.util.Map<java.lang.String, byte [ ]> key为文件名，Value为文件二进制流
     */
    public static Map<String, byte[]> getFileMap(String filePath) {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        String fileName = "";
        byte[] fileB = null;
        Map<String, byte[]> result = new HashMap<>();
        try {
            File file = getFilePath(filePath);
            if (!file.exists() || !file.isFile()) {
                return null;
            }
            fileName = file.getName();
            fis = new FileInputStream(file);
            Long fileLong = file.length();
            bos = new ByteArrayOutputStream(fileLong.intValue());
            byte[] b = new byte[256];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fileB = bos.toByteArray();
            if (bos != null) {
                bos.close();
            }
            result.put(fileName, fileB);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }

        }
        return result;
    }

    /**
     * 二进制流转流文件
     *
     * @param buf 二进制流
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: java.io.InputStream 流文件
     */
    public static final InputStream byte2Input(byte[] buf) {
        if (buf == null) {
            return null;
        }
        return new ByteArrayInputStream(buf);
    }

    /**
     * 输入流文件转二进制流
     *
     * @param inStream 流文件
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: byte[] 二进制流
     */
    public static final byte[] input2byte(InputStream inStream)
            throws IOException {
        ByteArrayOutputStream baos = null;
        if (inStream == null) {
            return null;
        }
        try {
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = inStream.read(buff, 0, 100)) > 0) {
                baos.write(buff, 0, rc);
            }
            byte[] in2b = baos.toByteArray();

            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                }
            }
            return in2b;
        } finally {
            if (baos != null) {
                baos.close();
            }
        }
    }


    /**
     * 输出流文件转二进制流，会把输出流关闭
     *
     * @param outStream 流文件
     * @Author: huangpeijun
     * @Date: 2021/8/17
     * @return: byte[] 二进制流
     */
    public static final byte[] output2byte(OutputStream outStream)
            throws IOException {
        ByteArrayOutputStream baos = null;
        if (outStream == null) {
            return null;
        }
        try {
            baos = (ByteArrayOutputStream) outStream;
            byte[] in2b = ((ByteArrayOutputStream) outStream).toByteArray();
            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                }
            }
            return in2b;
        } finally {
            if (baos != null) {
                baos.close();
            }
        }
    }

    /**
     * 加密 字节转string
     *
     * @param key
     * @return
     */
    public static String encodeBase64String(byte[] key) {
        return Base64.encodeBase64String(key);
    }

    /**
     * 加密 字节转string
     *
     * @param key
     * @return
     */
    public static String encodeBase64Str(byte[] key) {
        try {
            return new String(Base64.encodeBase64(key), ENCODE);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * 解密，string转字节
     *
     * @param key
     * @return
     */
    public static byte[] decodeBase64(String key) {
        try {
            return Base64.decodeBase64(key.getBytes(ENCODE));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }


}
