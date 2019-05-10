package com.yjj.webpackagekit.core;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * created by yangjianjun on 2018/10/27
 * index entity
 */
public class ResourceInfoEntity {
    private String version;
    private String packageId;

    @Expose(deserialize = false, serialize = false) private String md5;
    private List<ResourceInfo> items;

    public String getVersion() {
        return version;
    }

    public List<ResourceInfo> getItems() {
        return items;
    }

    public String getPackageId() {
        return packageId;
    }
}
