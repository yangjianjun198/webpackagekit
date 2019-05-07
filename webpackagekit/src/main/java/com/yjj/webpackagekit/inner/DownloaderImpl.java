package com.yjj.webpackagekit.inner;

import android.content.Context;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.yjj.webpackagekit.core.Downloader;
import com.yjj.webpackagekit.core.PackageInfo;
import com.yjj.webpackagekit.core.util.FileUtils;

/**
 * created by yangjianjun on 2018/10/27
 * 下载器实现类
 */
public class DownloaderImpl implements Downloader {
    private Context context;

    public DownloaderImpl(Context context) {
        this.context = context;
    }

    @Override
    public void download(PackageInfo packageInfo, final DownloadCallback callback) {
        BaseDownloadTask downloadTask = FileDownloader.getImpl()
            .create(packageInfo.getDownloadUrl())
            .setTag(packageInfo.getPackageId())
            .setPath(FileUtils.getPackageDownloadName(context, packageInfo.getPackageId()))
            .addFinishListener(new BaseDownloadTask.FinishListener() {
                @Override
                public void over(BaseDownloadTask task) {
                    if (callback != null && task.getStatus() == FileDownloadStatus.completed) {
                        callback.onSuccess((String) task.getTag());
                    } else if (callback != null) {
                        callback.onFailure((String) task.getTag());
                    }
                }
            });
        downloadTask.start();
    }
}
