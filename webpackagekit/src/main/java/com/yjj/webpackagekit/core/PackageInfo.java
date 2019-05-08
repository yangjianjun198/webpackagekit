package com.yjj.webpackagekit.core;

import android.text.TextUtils;

/**
 * created by yangjianjun on 2018/10/25
 * 离线包信息
 */
public class PackageInfo {
    /**
     * 离线包ID
     */
    private String packageId;
    /***
     * 离线包下载地址
     * */
    private String downloadUrl;
    /***
     *
     * 离线包版本号
     * */
    private String version = "1.0";

    /***
     * 离线包的状态 {@link PackageStatus}
     * */
    private int status = PackageStatus.onLine;

    /**
     * 是否是patch包
     */
    private boolean isPatch;

    /**
     * 离线包md值 由后端下发
     */
    private String md5;

    public String getPackageId() {
        return packageId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getVersion() {
        return version;
    }

    public int getStatus() {
        return status;
    }

    public boolean isPatch() {
        return isPatch;
    }

    public String getMd5() {
        return md5;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageInfo)) {
            return false;
        }
        PackageInfo that = (PackageInfo) obj;
        return TextUtils.equals(packageId, that.packageId);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + packageId == null ? 0 : packageId.hashCode();
        return result;
    }
}
