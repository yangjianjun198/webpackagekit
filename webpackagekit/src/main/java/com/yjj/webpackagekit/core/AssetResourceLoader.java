package com.yjj.webpackagekit.core;

/**
 * created by yangjianjun on 2019/5/10
 * asset资源加载器
 */
public interface AssetResourceLoader {
    /**
     * asset资源路径信息
     * @param path
     */
    PackageInfo load(String path);
}
