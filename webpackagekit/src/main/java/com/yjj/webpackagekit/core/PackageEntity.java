package com.yjj.webpackagekit.core;

import java.util.List;

/**
 * created by yangjianjun on 2018/10/27
 * 离线包Index信息
 */
public class PackageEntity {
    private List<PackageInfo> items;

    public void setItems(List<PackageInfo> items) {
        this.items = items;
    }

    public List<PackageInfo> getItems() {
        return items;
    }
}
