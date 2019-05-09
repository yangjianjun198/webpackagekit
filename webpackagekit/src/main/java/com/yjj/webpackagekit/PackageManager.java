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
import com.yjj.webpackagekit.core.PackageStatus;
import com.yjj.webpackagekit.core.ResourceManager;
import com.yjj.webpackagekit.core.ResoureceValidator;
import com.yjj.webpackagekit.core.util.FileUtils;
import com.yjj.webpackagekit.core.util.GsonUtils;
import com.yjj.webpackagekit.core.util.Logger;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * created by yangjianjun on 2018/10/24
 * 离线包管理器
 */
public class PackageManager {
    private static final int WHAT_DOWNLOAD_SUCCESS = 1;
    private static final int WHAT_DOWNLOAD_FAILURE = 2;
    private static final int WHAT_START_UPDATE = 3;

    private static final int STATUS_PACKAGE_CANUSE = 1;
    private volatile static PackageManager instance;

    private Context context;
    private ResourceManager resourceManager;
    private PackageInstaller packageInstaller;
    private volatile boolean isUpdating = false;
    private Handler packageHandler;
    private HandlerThread packageThread;
    private PackageEntity localPackageEntity;
    /**
     * 即将下载的packageInfoList
     */
    private List<PackageInfo> willDownloadPackageInfoList;
    /***
     * 需要更新资源
     * */
    private List<PackageInfo> onlyUpdatePackageInfoList;
    private Lock resouceLock;
    private PackageValidator validator;
    private Map<String, Integer> packageStatusMap = new HashMap<>();

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
        validator = new DefaultPackageValidator(context);
    }

    public void setResouceValidator(ResoureceValidator validator) {
        resourceManager.setResourceValidator(validator);
    }

    public void setPackageValidator(PackageValidator validator) {
        this.validator = validator;
    }

    private PackageManager() {
        resouceLock = new ReentrantLock();
    }

    /**
     * 更新离线包信息
     *
     * @param packageStr package json字符串
     */
    public void update(String packageStr) {
        if (isUpdating) {
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

    /**
     * package thread执行
     *
     * @param packageStr json 字符串
     */
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
        willDownloadPackageInfoList = new ArrayList<>(2);
        if (netEntity != null && netEntity.getItems() != null) {
            willDownloadPackageInfoList.addAll(netEntity.getItems());
        }
        /**
         * 不是第一次Load package
         */
        if (!isFirstLoadPackage) {
            initLocalEntity(packageIndexFile);
        }
        List<PackageInfo> packageInfoList = new ArrayList<>(willDownloadPackageInfoList.size());
        for (PackageInfo packageInfo : willDownloadPackageInfoList) {
            if (packageInfo.getStatus() == PackageStatus.offLine) {
                continue;
            }
            packageInfoList.add(packageInfo);
        }
        willDownloadPackageInfoList.clear();
        willDownloadPackageInfoList.addAll(packageInfoList);

        for (PackageInfo packageInfo : willDownloadPackageInfoList) {
            Downloader downloader = new DownloaderImpl(context);
            downloader.download(packageInfo, new DownloadCallback(this));
        }
        if (onlyUpdatePackageInfoList != null && onlyUpdatePackageInfoList.size() > 0) {
            for (PackageInfo packageInfo : onlyUpdatePackageInfoList) {
                resourceManager.updateResource(packageInfo.getPackageId());
                updateIndexFile(packageInfo.getPackageId(), packageInfo.getVersion());
                synchronized (packageStatusMap) {
                    packageStatusMap.put(packageInfo.getPackageId(), STATUS_PACKAGE_CANUSE);
                }
            }
        }
    }

    private void initLocalEntity(File packageIndexFile) {
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(packageIndexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            return;
        }
        localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        if (localPackageEntity == null || localPackageEntity.getItems() == null) {
            return;
        }
        int index = 0;
        for (PackageInfo localInfo : localPackageEntity.getItems()) {
            if ((index = willDownloadPackageInfoList.indexOf(localInfo)) < 0) {
                continue;
            }
            PackageInfo info = willDownloadPackageInfoList.get(index);
            if (VersionUtils.compareVersion(info.getVersion(), localInfo.getVersion()) <= 0) {
                if (!checkResourceFileValid(info.getPackageId())) {
                    return;
                }
                willDownloadPackageInfoList.remove(index);
                if (onlyUpdatePackageInfoList == null) {
                    onlyUpdatePackageInfoList = new ArrayList<>(2);
                }
                if (info.getStatus() == PackageStatus.onLine) {
                    onlyUpdatePackageInfoList.add(localInfo);
                }
                localInfo.setStatus(info.getStatus());
            } else {
                localInfo.setStatus(info.getStatus());
                localInfo.setVersion(info.getVersion());
            }
        }
    }

    private boolean checkResourceFileValid(String packageId) {
        File indexFile = FileUtils.getResourceIndexFile(context, packageId);
        return indexFile.exists() && indexFile.isFile();
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
        if (localPackageEntity == null) {
            FileInputStream indexFis = null;
            try {
                indexFis = new FileInputStream(packageIndexFile);
            } catch (FileNotFoundException e) {

            }
            if (indexFis == null) {
                return;
            }
            localPackageEntity = GsonUtils.fromJsonIgnoreException(indexFis, PackageEntity.class);
        }
        if (localPackageEntity == null) {
            localPackageEntity = new PackageEntity();
        }
        List<PackageInfo> packageInfoList = new ArrayList<>(2);
        if (localPackageEntity.getItems() != null) {
            packageInfoList.addAll(localPackageEntity.getItems());
        }
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.setPackageId(packageId);
        if (packageInfoList.indexOf(packageInfo) >= 0) {
            packageInfo.setVersion(version);
        } else {
            packageInfo.setStatus(PackageStatus.onLine);
            packageInfo.setVersion(version);
            packageInfoList.add(packageInfo);
        }
        localPackageEntity.setItems(packageInfoList);
        if (localPackageEntity == null || localPackageEntity.getItems() == null
            || localPackageEntity.getItems().size() == 0) {
            return;
        }
        String updateStr = new Gson().toJson(localPackageEntity);
        try {
            FileOutputStream outputStream = new FileOutputStream(packageIndexFile);
            try {
                outputStream.write(updateStr.getBytes());
            } catch (IOException ignore) {
                Logger.e("write packageIndex file error");
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
            Logger.e("read packageIndex file error");
        }
    }

    public WebResourceResponse getResource(String url) {
        synchronized (packageStatusMap) {
            String packageId = resourceManager.getPackageId(url);
            int status = packageStatusMap.get(packageId);
            if (status != STATUS_PACKAGE_CANUSE) {
                return null;
            }
        }
        WebResourceResponse resourceResponse = null;
        if (!resouceLock.tryLock()) {
            return null;
        }
        resourceResponse = resourceManager.getResource(url);
        resouceLock.unlock();
        return resourceResponse;
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
        PackageInfo tmp = new PackageInfo();
        tmp.setPackageId(packageId);
        int pos = willDownloadPackageInfoList.indexOf(tmp);
        if (pos >= 0) {
            packageInfo = willDownloadPackageInfoList.remove(pos);
        }
        allResouceUpdateFinished();
        /**
         * 安装
         * */
        if (packageInfo != null) {
            if (validator.validate(packageInfo)) {
                resouceLock.lock();
                boolean isSuccess = packageInstaller.install(packageInfo);
                resouceLock.unlock();
                /**
                 * 安装失败情况下，不做任何处理，因为资源既然资源需要最新资源，失败了，就没有必要再用缓存了
                 */
                if (isSuccess) {
                    resourceManager.updateResource(packageInfo.getPackageId());
                    updateIndexFile(packageInfo.getPackageId(), packageInfo.getVersion());
                    synchronized (packageStatusMap) {
                        packageStatusMap.put(packageId, STATUS_PACKAGE_CANUSE);
                    }
                }
            }
        }
    }

    /**
     * 处理离线下载失败
     * 在package thread中执行
     *
     * @param packageId 离线包id
     */
    private void performDownloadFailure(String packageId) {
        if (willDownloadPackageInfoList == null) {
            return;
        }
        int pos = willDownloadPackageInfoList.indexOf(packageId);
        if (pos >= 0) {
            willDownloadPackageInfoList.remove(pos);
        }
        allResouceUpdateFinished();
    }

    private void allResouceUpdateFinished() {
        if (willDownloadPackageInfoList.size() == 0) {
            isUpdating = false;
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

    static class DownloadCallback implements Downloader.DownloadCallback {
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

    static class DefaultPackageValidator implements PackageValidator {
        private Context context;

        public DefaultPackageValidator(Context context) {
            this.context = context;
        }

        @Override
        public boolean validate(PackageInfo packageInfo) {
            String downloadFilePath = FileUtils.getPackageDownloadName(context, packageInfo.getPackageId());
            File downloadFile = new File(downloadFilePath);
            if (downloadFile.exists() && MD5Utils.checkMD5(packageInfo.getMd5(), downloadFile)) {
                return true;
            }
            return false;
        }
    }
}
