# webpackagekit
webview 轻量离线包 
## 1、webview 支持离线资源加载
## 2、支持增量更新资源
## 3、支持预置包

demo使用说明
#### 1、webpackagekit 初始化
建议在application进行初始化,PackageManager.getInstance().init
#### 2、webpackagekit资源初始化
建议后端接入者服务端返回指定格式的数据信息.
PackageManager.getInstance().update(json)
#### 3、预置内置包
webpackagekit 默认内置包名为packageApp.zip。可以定制。

