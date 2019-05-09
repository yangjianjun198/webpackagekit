package com.yjj.webpackagekit;

import com.yjj.webpackagekit.core.PackageInfo;

/**
 * created by yangjianjun on 2019/5/9
 * 校验资源信息的有效性
 */
public interface PackageValidator {
    boolean validate(PackageInfo info);
}
