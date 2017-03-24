
![](https://github.com/qyxxjd/ClearProcesses/blob/master/screenshots/screenrecord.gif)


#### [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService.html)
###### 1.简介

```java
public abstract class AccessibilityService extends Service

java.lang.Object
 ↳ android.content.Context
   ↳ android.content.ContextWrapper
     ↳ android.app.Service
       ↳ android.accessibilityservice.AccessibilityService
```
> 无障碍服务旨在帮助身心有障碍的用户使用Android设备和应用。无障碍服务在后台运行，当无障碍事件被激活时系统会执行`AccessibilityService`的`onAccessibilityEvent(AccessibilityEvent event)`方法。这些事件表示在用户界面中的一些状态的改变，例如：焦点的改变、按钮被点击等。这类服务可以有选择性地请求查询活动窗口的内容。无障碍服务的开发需要继承`AccessibilityService`和实现它的抽象方法。

###### 2.使用
```java
public class YourService extends AccessibilityService {
    @Override public void onAccessibilityEvent(final AccessibilityEvent event) {
        //TODO ...
    }
    @Override public void onInterrupt() {
	      //TODO ...
    }
}
```
在`onAccessibilityEvent`方法中，通过`AccessibilityEvent`的`getSource`方法获取`AccessibilityNodeInfo`UI节点信息
```java
AccessibilityNodeInfo nodeInfo = event.getSource();
```
通过`AccessibilityNodeInfo`的`findAccessibilityNodeInfosByText` 、
`findAccessibilityNodeInfosByViewId`方法获取你感兴趣的UI子节点信息
```java
List<AccessibilityNodeInfo> nodeInfoList = event.getSource().findAccessibilityNodeInfosByText("强行停止");
```
通过`AccessibilityNodeInfo`的`performAction`方法来模拟用户点击事件
```java
nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
```
使用`AccessibilityService`之前需要判断一下当前是否已经授权
```java
private boolean checkEnabledAccessibilityService() {
    List<AccessibilityServiceInfo> accessibilityServices =
            mAccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
    for (AccessibilityServiceInfo info : accessibilityServices) {
        if (info.getId().equals(SERVICE_NAME)) {
            return true;
        }
    }
    return false;
}
```
如果没有授权，需要到系统设置页面进行设置
```java
Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
startActivity(intent);
```
###### 3.属性配置
在资源文件`res`目录下新建一个`xml`文件夹,创建`AccessibilityService`配置文件`your_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/app_name"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:packageNames="com.android.settings"
    android:accessibilityFeedbackType="feedbackAllMask"
    android:notificationTimeout="500"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="true"
    />
```

| 属性名称 | 属性简介 |
| ---- | ---- |
| `android:accessibilityEventTypes` | 指定我们在监听窗口中可以模拟哪些事件 |
| `android:accessibilityFeedbackType` | 指定无障碍服务的反馈方式 |
| `android:accessibilityFlags` | 指定额外的标志 |
| `android:canRetrieveWindowContent` | 指定是否允许我们的程序读取窗口中的节点和内容 |
| `android:description` | 系统设置无障碍页面显示的选项名称 |
| `android:notificationTimeout` | 两个相同类型事件发送的时间间隔，单位毫秒 |
| `android:packageNames` | 指定监听的应用程序包名，多个以`,`隔开 |
更多属性介绍请参考：[AccessibilityServiceInfo](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo.html)

###### 4.AndroidManifest配置
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    ... >

    <application ...>
        ...
        <service
            android:name=".YourService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"> <-- 需要用到BIND_ACCESSIBILITY_SERVICE这个权限 -->
            <intent-filter>
                <-- 有了这个action，用户才能在设置里面看到我们的服务 -->
                <action android:name="android.accessibilityservice.AccessibilityService"/>
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/your_config"/>
        </service>
	...
    </application>

</manifest>
```

#### 清理后台进程
###### 1.获取后台进程
通过`ActivityManager`的`getRunningAppProcesses`和`getRunningServices`方法获取后台运行的应用程序、服务列表
```java
ActivityManager mActivityManager = (ActivityManager) mAppContext.getSystemService(Context.ACTIVITY_SERVICE);
//返回在设备上运行的应用程序的进程的列表
List<ActivityManager.RunningAppProcessInfo> appProcessInfos = mActivityManager.getRunningAppProcesses();
//返回当前正在运行的服务的列表
List<ActivityManager.RunningServiceInfo> serviceInfos = mActivityManager.getRunningServices(yourCount);
```
###### 2.进入应用程序详情页面
```java
private void showPackageDetail(String packageName){
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts(PACKAGE, packageName, null);
    intent.setData(uri);
    startActivity(intent);
}
```
###### 3.模拟强行停止操作
```java
@TargetApi(Build.VERSION_CODES.JELLY_BEAN) @Override public void onAccessibilityEvent(final AccessibilityEvent event) {
    if(null == event || null == event.getSource()) { return; }
    if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.getPackageName().equals(PACKAGE)){
        final CharSequence className = event.getClassName();
        if(className.equals(NAME_APP_DETAILS)){ //模拟点击强行停止按钮
            simulationClick(event, TEXT_FORCE_STOP);
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
        if(className.equals(NAME_ALERT_DIALOG)){ //模拟点击弹窗的确认按钮
            simulationClick(event, TEXT_DETERMINE);
            performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN) private void simulationClick(AccessibilityEvent event, String text){
    List<AccessibilityNodeInfo> nodeInfoList = event.getSource().findAccessibilityNodeInfosByText(text);
    for (AccessibilityNodeInfo node : nodeInfoList) {
        if (node.isClickable() && node.isEnabled()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }
}
```

## 关于

* Blog: [http://blog.csdn.net/qy1387](http://blog.csdn.net/qy1387)
* Email: [pgliubin@gmail.com](http://mail.qq.com/cgi-bin/qm_share?t=qm_mailme&email=pgliubin@gmail.com)

## License

```
Copyright 2015 classic

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
