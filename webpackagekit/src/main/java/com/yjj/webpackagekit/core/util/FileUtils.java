package com.yjj.webpackagekit.core.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.yjj.webpackagekit.core.Contants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * created by yangjianjun on 2018/10/25
 * file 工具类
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * 根据fileName获取inputStream
     */
    public static InputStream getInputStream(String fileName) {

        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }
        if (file.isDirectory()) {
            return null;
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (Exception e) {

        }
        if (fileInputStream == null) {
            return null;
        }
        return new BufferedInputStream(fileInputStream);
    }

    /**
     * 解压zip到指定的路径
     *
     * @param zipFileString ZIP的名称
     * @param outPathString 要解压缩路径
     * @throws Exception
     */
    public static void unZipFolder(String zipFileString, String outPathString) throws Exception {
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if (zipEntry.isDirectory()) {
                //获取部件的文件夹名
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPathString + File.separator + szName);
                folder.mkdirs();
            } else {
                Log.e(TAG, outPathString + File.separator + szName);
                File file = new File(outPathString + File.separator + szName);
                if (!file.exists()) {
                    Log.e(TAG, "Create the file:" + outPathString + File.separator + szName);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                // 获取文件的输出流
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                // 读取（字节）字节到缓冲区
                while ((len = inZip.read(buffer)) != -1) {
                    // 从缓冲区（0）位置写入（字节）字节
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inZip.close();
    }


    /**
     * 获取缓存目录
     */
    public static File getFileDirectory(Context context, boolean preferExternal) {
        File appCacheDir = null;
        if (preferExternal && isExternalStorageMounted()) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getFilesDir();
        }
        if (appCacheDir == null) {
            String cacheDirPath = "/data/data/" + context.getPackageName() + "/file/";
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }

    private static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "file");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                return null;
            }
        }
        return appCacheDir;
    }

    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equalsIgnoreCase(getExternalStorageState());
    }

    public static String getExternalStorageState() {
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens
            externalStorageState = "";
        }
        return externalStorageState;
    }

    /**
     * 获取根容器的地址
     */
    public static String getPackageRootPath(Context context) {
        File fileDir = getFileDirectory(context, true);
        if (fileDir == null) {
            return null;
        }

        String path = fileDir + File.separator + Contants.PACKAGE_FILE_ROOT_PATH;
        File file;
        if (!(file = new File(path)).exists()) {
            file.mkdirs();
        }
        return path;
    }

    /**
     * 获取根容器的地址
     */
    public static String getPackageLoadPath(Context context, String packageId) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        return root + File.separator + packageId;
    }

    /***
     * 根据packageId获取work地址
     * */
    public static String getPackageWorkName(Context context, String packageId) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        return root + File.separator + packageId + File.separator + Contants.PACKAGE_WORK;
    }

    /***
     * 根据packageId获取package.json地址
     * */
    public static String getPackageIndexFileName(Context context) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        makeDir(root);
        return root + File.separator + Contants.PACKAGE_FILE_PACKAGE_INDEX;
    }

    /***
     * 根据packageId获取update地址
     * */
    public static String getPackageUpdateName(Context context, String packageId) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        return root + File.separator + packageId + File.separator + Contants.PACKAGE_UPDATE;
    }

    /***
     * 根据packageId获取下载目录文件
     * */
    public static String getPackageDownloadName(Context context, String packageId) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        return root + File.separator + packageId + File.separator + Contants.PACKAGE_DOWNLOAD;
    }

    /***
     * 根据packageId获取下载目录文件
     * */
    public static String getPackageMergePatch(Context context, String packageId) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        return root + File.separator + packageId + File.separator + Contants.PACKAGE_MERGE;
    }

    /***
     * 根据packageId获取update_tmp地址
     * */
    public static String getPackageUpdareTempName(Context context, String packageId) {
        String root = getPackageRootPath(context);
        if (TextUtils.isEmpty(root)) {
            return null;
        }
        return root + File.separator + packageId + File.separator + Contants.PACKAGE_UPDATE_TEMP;
    }

    /**
     * 复制单个文件
     *
     * @param srcFileName 待复制的文件名
     * @param descFileName 目标文件名
     * @return 如果复制成功，则返回true，否则返回false
     */
    public static boolean copyFileCover(String srcFileName, String descFileName) {
        File srcFile = new File(srcFileName);
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            return false;
        }
        // 判断源文件是否是合法的文件
        else if (!srcFile.isFile()) {
            return false;
        }
        File descFile = new File(descFileName);
        // 判断目标文件是否存在
        if (descFile.exists()) {
            if (!FileUtils.delFile(descFileName)) {
                return false;
            }
        } else if (descFile.getParentFile() != null) {
            if (!descFile.getParentFile().exists()) {
                // 如果目标文件所在的目录不存在，则创建目录
                if (!descFile.getParentFile().mkdirs()) {
                    return false;
                }
            }
        } else {
            return false;
        }

        // 准备复制文件
        // 读取的位数
        int readByte = 0;
        InputStream ins = null;
        OutputStream outs = null;
        try {
            // 打开源文件
            ins = new FileInputStream(srcFile);
            // 打开目标文件的输出流
            outs = new FileOutputStream(descFile);
            byte[] buf = new byte[1024];
            // 一次读取1024个字节，当readByte为-1时表示文件已经读取完毕
            while ((readByte = ins.read(buf)) != -1) {
                // 将读取的字节流写入到输出流
                outs.write(buf, 0, readByte);
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            // 关闭输入输出流，首先关闭输出流，然后再关闭输入流
            if (outs != null) {
                try {
                    outs.close();
                } catch (IOException oute) {
                    oute.printStackTrace();
                }
            }
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ine) {
                    ine.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件，可以删除单个文件或文件夹
     *
     * @param fileName 被删除的文件名
     * @return 如果删除成功，则返回true，否是返回false
     */
    public static boolean delFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return true;
        } else {
            if (file.isFile()) {
                return FileUtils.deleteFile(fileName);
            }
        }
        return true;
    }

    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * 复制目录
     * @param srcFile
     * @param dstFile
     * @return
     */
    public static boolean copyDir(String srcFile, String dstFile) {
        // 地址相等不复制
        if (TextUtils.equals(srcFile, dstFile)) {
            return true;
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File dst = new File(dstFile);
            if (!dst.getParentFile().exists()) {
                if (!dst.getParentFile().mkdirs()) {
                    return false;
                }
            }
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(dstFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }

    public static boolean makeDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

}
