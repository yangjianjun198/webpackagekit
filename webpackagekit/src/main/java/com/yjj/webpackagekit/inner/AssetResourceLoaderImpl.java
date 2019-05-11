package com.yjj.webpackagekit.inner;

import android.content.Context;
import android.text.TextUtils;

import com.yjj.webpackagekit.core.AssetResourceLoader;
import com.yjj.webpackagekit.core.PackageInfo;
import com.yjj.webpackagekit.core.PackageStatus;
import com.yjj.webpackagekit.core.ResourceInfoEntity;
import com.yjj.webpackagekit.core.util.FileUtils;
import com.yjj.webpackagekit.core.util.GsonUtils;
import com.yjj.webpackagekit.core.util.MD5Utils;
import com.yjj.webpackagekit.core.util.VersionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * created by yangjianjun on 2019/5/10
 * asset 资源加载
 */
public class AssetResourceLoaderImpl implements AssetResourceLoader {
    private Context context;

    public AssetResourceLoaderImpl(Context context) {
        this.context = context;
    }

    @Override
    public PackageInfo load(String path) {
        String assetPath = FileUtils.getPackageAssetsName(context);
        InputStream inputStream = null;

        inputStream = openAssetInputStream(path);
        String indexInfo = FileUtils.getStringForZip(inputStream);
        if (TextUtils.isEmpty(indexInfo)) {
            return null;
        }
        ResourceInfoEntity assetEntity = GsonUtils.fromJsonIgnoreException(indexInfo, ResourceInfoEntity.class);
        if (assetEntity == null) {
            return null;
        }
        inputStream = openAssetInputStream(path);
        if (inputStream == null) {
            return null;
        }
        File file = new File(FileUtils.getPackageUpdateName(context, assetEntity.getPackageId()));
        ResourceInfoEntity localEntity = null;
        FileInputStream fileInputStream = null;
        if (file.exists()) {
            try {
                fileInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {

            }
        }
        String lo = null;
        if (fileInputStream != null) {
            lo = FileUtils.getStringForZip(fileInputStream);
        }
        if (!TextUtils.isEmpty(lo)) {
            localEntity = GsonUtils.fromJsonIgnoreException(lo, ResourceInfoEntity.class);
        }
        if (localEntity != null
            && VersionUtils.compareVersion(assetEntity.getVersion(), localEntity.getVersion()) <= 0) {
            return null;
        }
        boolean isSuccess = FileUtils.copyFile(inputStream, assetPath);
        if (!isSuccess) {
            return null;
        }
        FileUtils.safeCloseFile(inputStream);
        String md5 = MD5Utils.calculateMD5(new File(assetPath));
        if (TextUtils.isEmpty(md5)) {
            return null;
        }
        PackageInfo info = new PackageInfo();
        info.setPackageId(assetEntity.getPackageId());
        info.setStatus(PackageStatus.onLine);
        info.setVersion(assetEntity.getVersion());
        info.setMd5(md5);
        return info;
    }

    private InputStream openAssetInputStream(String path) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(path);
        } catch (IOException e) {
        }
        if (inputStream == null) {
            return null;
        }
        return inputStream;
    }
}
