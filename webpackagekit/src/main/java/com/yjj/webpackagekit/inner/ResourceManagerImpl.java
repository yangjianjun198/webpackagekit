package com.yjj.webpackagekit.inner;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import com.yjj.webpackagekit.core.Contants;
import com.yjj.webpackagekit.core.ResourceInfo;
import com.yjj.webpackagekit.core.ResourceInfoEntity;
import com.yjj.webpackagekit.core.ResourceKey;
import com.yjj.webpackagekit.core.ResourceManager;
import com.yjj.webpackagekit.core.ResoureceValidator;
import com.yjj.webpackagekit.core.util.FileUtils;
import com.yjj.webpackagekit.core.util.GsonUtils;
import com.yjj.webpackagekit.core.util.Logger;
import com.yjj.webpackagekit.core.util.MD5Utils;
import com.yjj.webpackagekit.core.util.MimeTypeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * created by yangjianjun on 2018/10/25
 * 资源管理类实现
 */
public class ResourceManagerImpl implements ResourceManager {
    private Map<ResourceKey, ResourceInfo> resourceInfoMap;
    private Context context;
    private Lock lock;
    private ResoureceValidator validator;

    public ResourceManagerImpl(Context context) {
        resourceInfoMap = new ConcurrentHashMap<>(16);
        this.context = context;
        lock = new ReentrantLock();
        validator = new DefaultResourceValidator();
    }

    /**
     * 获取资源信息
     * 会做md5校验
     *
     * @param url 请求地址
     */
    @Override
    public WebResourceResponse getResource(String url) {
        ResourceKey key = new ResourceKey(url);
        if (!lock.tryLock()) {
            return null;
        }
        ResourceInfo resourceInfo = resourceInfoMap.get(key);
        lock.unlock();
        if (resourceInfo == null) {
            return null;
        }
        if (!MimeTypeUtils.checkIsSupportMimeType(resourceInfo.getMimeType())) {
            Logger.d("getResource [" + url + "]" + " is not support mime type");
            safeRemoveResource(key);
            return null;
        }
        InputStream inputStream = FileUtils.getInputStream(resourceInfo.getLocalPath());
        if (inputStream == null) {
            Logger.d("getResource [" + url + "]" + " inputStream is null");
            safeRemoveResource(key);
            return null;
        }
        if (validator != null && !validator.validate(resourceInfo)) {
            safeRemoveResource(key);
            return null;
        }
        WebResourceResponse response;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, String> header = new HashMap<>(2);
            header.put("Access-Control-Allow-Origin", "*");
            header.put("Access-Control-Allow-Headers", "Content-Type");
            response = new WebResourceResponse(resourceInfo.getMimeType(), "UTF-8", 200, "ok", header, inputStream);
        } else {
            response = new WebResourceResponse(resourceInfo.getMimeType(), "UTF-8", inputStream);
        }
        return response;
    }

    private void safeRemoveResource(ResourceKey key) {
        if (lock.tryLock()) {
            resourceInfoMap.remove(key);
            lock.unlock();
        }
    }

    @Override
    public boolean updateResource(String packageId) {
        boolean isSuccess = false;
        String indexFileName =
            FileUtils.getPackageWorkName(context, packageId) + File.separator + Contants.RESOURCE_MIDDLE_PATH
                + File.separator + Contants.RESOURCE_INDEX_NAME;
        Logger.d("updateResource indexFileName: " + indexFileName);
        File indexFile = new File(indexFileName);
        if (!indexFile.exists()) {
            Logger.e("updateResource indexFile is not exists ,update Resource error ");
            return isSuccess;
        }
        if (!indexFile.isFile()) {
            Logger.e("updateResource indexFile is not file ,update Resource error ");
            return isSuccess;
        }
        FileInputStream indexFis = null;
        try {
            indexFis = new FileInputStream(indexFile);
        } catch (FileNotFoundException e) {

        }
        if (indexFis == null) {
            Logger.e("updateResource indexStream is error,  update Resource error ");
            return isSuccess;
        }

        ResourceInfoEntity entity = GsonUtils.fromJsonIgnoreException(indexFis, ResourceInfoEntity.class);
        if (indexFis != null) {
            try {
                indexFis.close();
            } catch (IOException e) {

            }
        }
        if (entity == null) {
            return isSuccess;
        }
        List<ResourceInfo> resourceInfos = entity.getItems();
        isSuccess = true;
        if (resourceInfos == null) {
            return isSuccess;
        }
        String workPath = FileUtils.getPackageWorkName(context, packageId);
        for (ResourceInfo resourceInfo : resourceInfos) {
            if (TextUtils.isEmpty(resourceInfo.getPath())) {
                continue;
            }
            resourceInfo.setPackageId(packageId);
            String path = resourceInfo.getPath();
            path = path.startsWith(File.separator) ? path.substring(1) : path;
            resourceInfo.setLocalPath(
                workPath + File.separator + Contants.RESOURCE_MIDDLE_PATH + File.separator + path);
            lock.lock();
            resourceInfoMap.put(new ResourceKey(resourceInfo.getRemoteUrl()), resourceInfo);
            lock.unlock();
        }
        return isSuccess;
    }

    @Override
    public void setResourceValidator(ResoureceValidator validator) {
        this.validator = validator;
    }

    @Override
    public String getPackageId(String url) {
        if (!lock.tryLock()) {
            return null;
        }
        ResourceInfo resourceInfo = resourceInfoMap.get(new ResourceKey(url));
        lock.unlock();
        if (resourceInfo != null) {
            return resourceInfo.getPackageId();
        }
        return null;
    }

    static class DefaultResourceValidator implements ResoureceValidator {
        @Override
        public boolean validate(ResourceInfo resourceInfo) {
            String rMd5 = resourceInfo.getMd5();
            if (!TextUtils.isEmpty(rMd5) && !MD5Utils.checkMD5(rMd5, new File(resourceInfo.getLocalPath()))) {
                return false;
            }
            int size = 0;
            try {
                InputStream inputStream = FileUtils.getInputStream(resourceInfo.getLocalPath());
                size = inputStream.available();
            } catch (IOException e) {
                Logger.e("resource file is error " + e.getMessage());
            }
            if (size == 0) {
                Logger.e("resource file is error ");
                return false;
            }
            return true;
        }
    }
}
