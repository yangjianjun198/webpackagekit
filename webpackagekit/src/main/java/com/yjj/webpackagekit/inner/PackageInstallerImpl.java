package com.yjj.webpackagekit.inner;

import android.content.Context;

import com.yjj.utils.DiffUtils;
import com.yjj.webpackagekit.core.PackageInfo;
import com.yjj.webpackagekit.core.PackageInstaller;
import com.yjj.webpackagekit.core.util.FileUtils;
import com.yjj.webpackagekit.core.util.Logger;

import java.io.File;

/**
 * created by yangjianjun on 2018/10/26
 * 单个离线包安装器
 */
public class PackageInstallerImpl implements PackageInstaller {
    private Context context;

    public PackageInstallerImpl(Context context) {
        this.context = context;
    }

    /**
     * 下载文件 download.zip
     * 如果是patch文件 merge.zip
     * 更新后的zip目录 update.zip
     *
     * @param packageInfo
     * @return
     */
    @Override
    public boolean install(PackageInfo packageInfo) {
        /**
         * 获取下载目录
         * */
        String downloadFile = FileUtils.getPackageDownloadName(context, packageInfo.getPackageId());
        String willCopyFile = downloadFile;
        /**
         * 获取update.zip名称
         * */
        String updateFile = FileUtils.getPackageUpdateName(context, packageInfo.getPackageId());
        boolean isSuccess = true;
        /**
         * merge离线增量
         */
        if (packageInfo.isPatch() && new File(updateFile).exists()) {
            String mergePatch = FileUtils.getPackageMergePatch(context, packageInfo.getPackageId());
            int status = DiffUtils.patch(downloadFile, updateFile, mergePatch);
            if (status == 0) {
                willCopyFile = mergePatch;
            } else {
                isSuccess = false;
            }
        }
        if (!isSuccess) {
            return false;
        }

        /***
         * 复制zip
         * */
        FileUtils.copyFileCover(willCopyFile, updateFile);
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "copy file error ");
            return false;
        }
        isSuccess = FileUtils.delFile(willCopyFile);
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "delete will copy file error ");
            return false;
        }
        /**
         * 获取解压目录
         * */
        String updatePath = updateFile.substring(0, updateFile.indexOf(".zip"));
        try {
            isSuccess = FileUtils.unZipFolder(updateFile, updatePath);
        } catch (Exception e) {
            isSuccess = false;
        }
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "unZipFolder error ");
            return false;
        }
        /**
         *
         * 解压成功，覆盖work下的内容
         */
        String workPath = FileUtils.getPackageWorkName(context, packageInfo.getPackageId());
        isSuccess = FileUtils.copyFolder(updatePath, workPath);
        if (isSuccess) {
            FileUtils.deleteDir(new File(updatePath));
            FileUtils.deleteFile(willCopyFile);
        }
        return isSuccess;
    }
}
