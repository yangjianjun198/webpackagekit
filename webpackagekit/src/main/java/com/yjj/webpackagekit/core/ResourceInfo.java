package com.yjj.webpackagekit.core;

import com.google.gson.annotations.Expose;

/**
 * created by yangjianjun on 2018/10/25
 * 资源信息，每个url对应的资源信息
 */
public class ResourceInfo {

    /**
     * 所关联的package id
     * 后续需要根据该id索引离线包信息
     */
    private String packageId;

    /***
     *
     *远端地址
     */
    private String remoteUrl;

    /**
     * 相对路径
     *
     * */
    private String path;

    @Expose(deserialize = false, serialize = false)
    /**
     * 本地加载地址
     * */ private String localPath;

    /**
     * 类型
     */
    private String mimeType;

    private String md5;

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getLocalPath() {
        return localPath;
    }


    public String getMimeType() {
        return mimeType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMd5() {
        return md5;
    }
}
