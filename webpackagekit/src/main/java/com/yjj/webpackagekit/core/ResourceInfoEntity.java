package com.yjj.webpackagekit.core;

import java.util.List;

/**
 * created by yangjianjun on 2018/10/27
 * index entity
 */
public class ResourceInfoEntity {
    private String version;
    private List<ResourceInfo> items;

    public String getVersion() {
        return version;
    }

    public List<ResourceInfo> getItems() {
        return items;
    }
}
