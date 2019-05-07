package com.yjj.demo;

import android.app.Application;

import com.yjj.webpackagekit.PackageManager;

/**
 * created by yangjianjun on 2019/5/7
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PackageManager.getInstance().init(this);
        PackageManager.getInstance().update(getPackageInfo());
    }

    private String getPackageInfo() {
        return "{\n" + "\t\"items\":[\n" + "\t\t{\n" + "\t\t\t\"packageId\":\"122\",\n"
            + "\t\t\t\"downloadUrl\":\"738738.zip\",\n" + "\t\t\t\"isPatch\":false\n" + "\t\t}\n" + "\t]\n" + "}";
    }
}
