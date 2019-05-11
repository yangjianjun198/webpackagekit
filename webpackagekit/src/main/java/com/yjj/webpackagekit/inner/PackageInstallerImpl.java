package com.yjj.webpackagekit.inner;

import android.content.Context;
import android.text.TextUtils;

import com.yjj.utils.DiffUtils;
import com.yjj.webpackagekit.core.PackageEntity;
import com.yjj.webpackagekit.core.PackageInfo;
import com.yjj.webpackagekit.core.PackageInstaller;
import com.yjj.webpackagekit.core.util.FileUtils;
import com.yjj.webpackagekit.core.util.GsonUtils;
import com.yjj.webpackagekit.core.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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
     */
    @Override
    public boolean install(PackageInfo packageInfo, boolean isAssets) {
        /**
         * 获取下载目录
         * */
        String downloadFile =
            isAssets ? FileUtils.getPackageAssetsName(context, packageInfo.getPackageId(), packageInfo.getVersion())
                : FileUtils.getPackageDownloadName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        String willCopyFile = downloadFile;
        /**
         * 获取update.zip名称
         * */
        String updateFile =
            FileUtils.getPackageUpdateName(context, packageInfo.getPackageId(), packageInfo.getVersion());
        boolean isSuccess = true;
        String lastVersion = getLastVersion(packageInfo.getPackageId());
        if (packageInfo.isPatch() && TextUtils.isEmpty(lastVersion)) {
            Logger.e("资源为patch ,但是上个版本信息没有数据，无法patch!");
            return false;
        }
        /**
         * merge离线增量
         */
        if (packageInfo.isPatch()) {
            String baseFile = FileUtils.getPackageUpdateName(context, packageInfo.getPackageId(), lastVersion);
            String mergePatch =
                FileUtils.getPackageMergePatch(context, packageInfo.getPackageId(), packageInfo.getVersion());
            int status = -1;
            try {
                status = DiffUtils.patch(baseFile, mergePatch, downloadFile);
            } catch (Exception ignore) {
                Logger.e("patch error " + ignore.getMessage());
            }
            if (status == 0) {
                willCopyFile = mergePatch;
                FileUtils.deleteFile(downloadFile);
            } else {
                isSuccess = false;
            }
        }
        if (!isSuccess) {
            Logger.e("资源patch merge 失败！");
            return false;
        }

        /***
         * 复制zip
         * */
        isSuccess = FileUtils.copyFileCover(willCopyFile, updateFile);
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
         *
         * 解压成功
         */
        String workPath = FileUtils.getPackageWorkName(context, packageInfo.getPackageId(), packageInfo.getVersion());

        try {
            isSuccess = FileUtils.unZipFolder(updateFile, workPath);
        } catch (Exception e) {
            isSuccess = false;
        }
        if (!isSuccess) {
            Logger.e("[" + packageInfo.getPackageId() + "] : " + "unZipFolder error ");
            return false;
        }
        if (isSuccess) {
            FileUtils.deleteFile(willCopyFile);
            cleanOldFileIfNeed(packageInfo.getPackageId(), packageInfo.getVersion(), lastVersion);
        }
        return isSuccess;
    }

    private void cleanOldFileIfNeed(String packageId, String version, String lastVersion) {
        String path = FileUtils.getPackageRootByPackageId(context, packageId);
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        File[] versionList = file.listFiles();
        if (versionList == null || versionList.length == 0) {
            return;
        }
        List<File> deleteFiles = new ArrayList<>();
        for (File item : versionList) {
            if (TextUtils.equals(version, item.getName()) || TextUtils.equals(lastVersion, item.getName())) {
                continue;
            }
            deleteFiles.add(item);
        }
        for (File file1 : deleteFiles) {
            FileUtils.deleteDir(file1);
        }
    }

    private String getLastVersion(String packageId) {
        String packageIndexFile = FileUtils.getPackageIndexFileName(context);
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(packageIndexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            return "";
        }
        PackageEntity localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        if (localPackageEntity == null || localPackageEntity.getItems() == null) {
            return "";
        }
        List<PackageInfo> list = localPackageEntity.getItems();
        PackageInfo info = new PackageInfo();
        info.setPackageId(packageId);
        int index = list.indexOf(info);
        if (index >= 0) {
            return list.get(index).getVersion();
        }
        return "";
    }
}
