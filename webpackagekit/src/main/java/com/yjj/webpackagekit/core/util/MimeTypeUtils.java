package com.yjj.webpackagekit.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * created by yangjianjun on 2018/10/25
 * mimeType工具类
 */
public class MimeTypeUtils {
    private static List<String> supportMineTypeList = new ArrayList<>(2);

    static {
        supportMineTypeList.add("application/x-javascript");
        supportMineTypeList.add("image/jpeg");
        supportMineTypeList.add("image/tiff");
        supportMineTypeList.add("text/css");
        supportMineTypeList.add("image/gif");
        supportMineTypeList.add("image/png");
    }

    public static boolean checkIsSupportMimeType(String mimeType) {
        return supportMineTypeList.contains(mimeType);
    }
}
