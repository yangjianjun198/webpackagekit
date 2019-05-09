package com.yjj.webpackagekit.core;

import android.webkit.WebResourceResponse;

/**
 * created by yangjianjun on 2018/10/24
 * 资源管理器
 */
public interface ResourceManager {
    WebResourceResponse getResource(String url);

    boolean updateResource(String packageId);

    void setResourceValidator(ResoureceValidator validator);

    String getPackageId(String url);
}
