package com.yjj.utils;

/**
 * created by yangjianjun on 2019/5/2
 */
public class DiffUtils {

    static {
        System.loadLibrary("diff-utils");
    }

    /**
     * native方法 比较路径为oldPath的文件与newPath的文件之间差异，并生成patch包，存储于patchPath
     * <p>
     * 返回：0，说明操作成功
     *
     * @param oldPath   示例:/sdcard/old.apk
     * @param newPath   示例:/sdcard/new.apk
     * @param patchPath 示例:/sdcard/xx.patch
     */
    public static native int genDiff(String oldPath, String newPath, String patchPath);

    /**
     * native方法 使用路径为oldPath的文件与路径为patchPath的补丁包，合成新的文件，并存储于newPath
     * <p>
     * 返回：0，说明操作成功
     *
     * @param oldPath   示例:/sdcard/old.apk
     * @param newPath   示例:/sdcard/new.apk
     * @param patchPath 示例:/sdcard/xx.patch
     */
    public static native int patch(String oldPath, String newPath, String patchPath);
}
