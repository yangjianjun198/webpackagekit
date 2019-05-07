package com.yjj.webpackagekit;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.webkit.WebResourceResponse;

import com.google.gson.Gson;
import com.yjj.webpackagekit.core.Downloader;
import com.yjj.webpackagekit.core.PackageEntity;
import com.yjj.webpackagekit.core.PackageInfo;
import com.yjj.webpackagekit.core.PackageInstaller;
import com.yjj.webpackagekit.core.ResourceManager;
import com.yjj.webpackagekit.core.util.FileUtils;
import com.yjj.webpackagekit.core.util.GsonUtils;
import com.yjj.webpackagekit.core.util.MD5Utils;
import com.yjj.webpackagekit.core.util.VersionUtils;
import com.yjj.webpackagekit.inner.DownloaderImpl;
import com.yjj.webpackagekit.inner.PackageInstallerImpl;
import com.yjj.webpackagekit.inner.ResourceManagerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * created by yangjianjun on 2018/10/24
 * 离线包管理器
 */
public class PackageManager {
    private static final int WHAT_DOWNLOAD_SUCCESS = 1;
    private static final int WHAT_DOWNLOAD_FAILURE = 2;
    private static final int WHAT_START_UPDATE = 3;

    private Context context;
    private ResourceManager resourceManager;
    private PackageInstaller packageInstaller;
    private volatile static PackageManager instance;
    private volatile boolean isInstalled = false;
    private Handler packageHandler;
    private HandlerThread packageThread;
    /**
     * 即将下载的packageInfoList
     */
    private List<PackageInfo> willDownloadPackageInfoList;
    /***
     * 需要更新资源
     * */
    private List<PackageInfo> onlyUpdatePackageInfoList;

    public static PackageManager getInstance() {
        if (instance == null) {
            synchronized (PackageManager.class) {
                if (instance == null) {
                    instance = new PackageManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
        resourceManager = new ResourceManagerImpl(context);
        packageInstaller = new PackageInstallerImpl(context);
    }

    /**
     * 更新离线包信息
     *
     * @param packageStr package json字符串
     */
    public void update(String packageStr) {
        if (isInstalled) {
            return;
        }
        if (packageStr == null) {
            packageStr = "";
        }
        if (packageThread == null) {
            packageThread = new HandlerThread("offline_package_thread");
            packageThread.start();
            packageHandler = new DownloadHandler(packageThread.getLooper());
        }
        Message message = Message.obtain();
        message.what = WHAT_START_UPDATE;
        message.obj = packageStr;
        packageHandler.sendMessage(message);
    }

    private void performUpdate(String packageStr) {
        String packageIndexFileName = FileUtils.getPackageIndexFileName(context);
        File packageIndexFile = new File(packageIndexFileName);
        /***
         * 是否是第一次加载离线包
         * */
        boolean isFirstLoadPackage = false;
        if (!packageIndexFile.exists()) {
            isFirstLoadPackage = true;
        }

        PackageEntity netEntity = null;
        netEntity = GsonUtils.fromJsonIgnoreException(packageStr, PackageEntity.class);
        willDownloadPackageInfoList = new CopyOnWriteArrayList<>();
        if (netEntity != null && netEntity.getItems() != null) {
            willDownloadPackageInfoList.addAll(netEntity.getItems());
        }
        /**
         * 不是第一次Load package
         */
        if (!isFirstLoadPackage) {
            FileInputStream indexFis = null;
            try {
                indexFis = new FileInputStream(packageIndexFile);
            } catch (FileNotFoundException e) {

            }
            if (indexFis == null) {
                return;
            }
            PackageEntity localEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
            if (localEntity == null) {
                return;
            }
            int index = 0;
            for (PackageInfo packageInfo : localEntity.getItems()) {
                if ((index = willDownloadPackageInfoList.indexOf(packageInfo)) >= 0) {
                    PackageInfo info = willDownloadPackageInfoList.get(index);
                    if (VersionUtils.compareVersion(info.getVersion(), packageInfo.getVersion()) <= 0) {
                        willDownloadPackageInfoList.remove(index);
                        if (onlyUpdatePackageInfoList == null) {
                            onlyUpdatePackageInfoList = new CopyOnWriteArrayList<>();
                        }
                        onlyUpdatePackageInfoList.add(packageInfo);
                    } else {
                        packageInfo.setVersion(info.getVersion());
                    }
                }
            }
        }
        for (PackageInfo packageInfo : willDownloadPackageInfoList) {
            Downloader downloader = new DownloaderImpl(context);
            downloader.download(packageInfo, new DownloadCallback(this));
        }
        if (willDownloadPackageInfoList.size() == 0 && onlyUpdatePackageInfoList != null
            && onlyUpdatePackageInfoList.size() > 0) {
            for (PackageInfo packageInfo : onlyUpdatePackageInfoList) {
                resourceManager.updateResource(packageInfo.getPackageId());
                updateIndexFile(packageInfo.getPackageId(), packageInfo.getVersion());
                isInstalled = true;
            }
        }
    }

    private void updateIndexFile(String packageId, String version) {
        String packageIndexFileName = FileUtils.getPackageIndexFileName(context);
        File packageIndexFile = new File(packageIndexFileName);
        if (!packageIndexFile.exists()) {
            boolean isSuccess = true;
            try {
                isSuccess = packageIndexFile.createNewFile();
            } catch (IOException e) {
                isSuccess = false;
            }
            if (!isSuccess) {
                return;
            }
        }
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(packageIndexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            return;
        }
        PackageEntity localEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        if (localEntity == null) {
            localEntity = new PackageEntity();
        }
        List<PackageInfo> packageInfoList = new ArrayList<>(2);
        if (localEntity.getItems() == null) {
            localEntity.setItems(packageInfoList);
        } else {
            packageInfoList = localEntity.getItems();
        }
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageId(packageId);
        if (packageInfoList.indexOf(packageInfo) >= 0) {
            packageInfo.setVersion(version);
        } else {
            packageInfo.setVersion(version);
            packageInfoList.add(packageInfo);
        }

        String updateStr = new Gson().toJson(localEntity);
        try {
            FileOutputStream outputStream = new FileOutputStream(packageIndexFile);
            try {
                outputStream.write(updateStr.getBytes());
            } catch (IOException ignore) {
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    public WebResourceResponse getResource(String url) {
        if (!isInstalled) {
            return null;
        }
        return resourceManager.getResource(url);
    }

    private void downloadSuccess(String packageId) {
        if (packageHandler == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = WHAT_DOWNLOAD_SUCCESS;
        message.obj = packageId;
        packageHandler.sendMessage(message);
    }

    private void downloadFailure(String packageId) {
        if (packageHandler == null) {
            return;
        }
        Message message = Message.obtain();
        message.what = WHAT_DOWNLOAD_FAILURE;
        message.obj = packageId;
        packageHandler.sendMessage(message);
    }

    private void performDownloadSuccess(String packageId) {
        if (willDownloadPackageInfoList == null) {
            return;
        }
        PackageInfo packageInfo = null;
        boolean isDownloadAll;
        synchronized (willDownloadPackageInfoList) {
            int pos = willDownloadPackageInfoList.indexOf(packageId);
            if (pos >= 0) {
                packageInfo = willDownloadPackageInfoList.remove(pos);
            }
            isDownloadAll = willDownloadPackageInfoList.size() == 0;
        }
        /**
         * 安装
         * */
        if (packageInfo != null) {
            String downloadFile = FileUtils.getPackageDownloadName(context, packageInfo.getPackageId());
            File downloadFilePath = new File(downloadFile);
            if (downloadFilePath.exists() && !MD5Utils.checkMD5(packageInfo.getMd5(), downloadFilePath)) {
                boolean isSuccess = packageInstaller.install(packageInfo);
                if (isSuccess) {
                    resourceManager.updateResource(packageInfo.getPackageId());
                    updateIndexFile(packageInfo.getPackageId(), packageInfo.getVersion());
                }
            }
        }

        /***
         * 全部下载完毕,开始更新原有的资源信息
         * */
        if (isDownloadAll && onlyUpdatePackageInfoList != null) {
            for (PackageInfo packageInfo1 : onlyUpdatePackageInfoList) {
                resourceManager.updateResource(packageInfo1.getPackageId());
                updateIndexFile(packageInfo1.getPackageId(), packageInfo1.getVersion());
            }
        }
        if (isDownloadAll) {
            isInstalled = true;
        }
    }

    /**
     * 处理离线下载失败
     *
     * @param packageId 离线包id
     */
    private void performDownloadFailure(String packageId) {
        if (willDownloadPackageInfoList == null) {
            return;
        }
        boolean isDownloadAll;
        synchronized (willDownloadPackageInfoList) {
            int pos = willDownloadPackageInfoList.indexOf(packageId);
            willDownloadPackageInfoList.remove(pos);
            isDownloadAll = willDownloadPackageInfoList.size() == 0;
        }

        if (isDownloadAll && onlyUpdatePackageInfoList != null) {
            for (PackageInfo packageInfo1 : onlyUpdatePackageInfoList) {
                resourceManager.updateResource(packageInfo1.getPackageId());
                updateIndexFile(packageInfo1.getPackageId(), packageInfo1.getVersion());
            }
        }
        if (isDownloadAll) {
            isInstalled = true;
        }
    }

    /**
     * 离线包handler处理器
     */
    class DownloadHandler extends Handler {
        public DownloadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_DOWNLOAD_SUCCESS:
                    performDownloadSuccess((String) msg.obj);
                    break;
                case WHAT_DOWNLOAD_FAILURE:
                    performDownloadFailure((String) msg.obj);
                    break;
                case WHAT_START_UPDATE:
                    performUpdate((String) msg.obj);
                default:
                    break;
            }
        }
    }

    private static class DownloadCallback implements Downloader.DownloadCallback {
        private PackageManager packageManager;

        public DownloadCallback(PackageManager packageManager) {
            this.packageManager = packageManager;
        }

        @Override
        public void onSuccess(String packageId) {
            packageManager.downloadSuccess(packageId);
        }

        @Override
        public void onFailure(String packageId) {
            packageManager.downloadFailure(packageId);
        }
    }
}
