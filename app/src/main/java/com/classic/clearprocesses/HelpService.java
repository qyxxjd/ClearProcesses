package com.classic.clearprocesses;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * 应用名称: ClearProcesses
 * 包 名 称: com.classic.clearprocesses
 *
 * 文件描述: TODO
 * 创 建 人: 续写经典
 * 创建时间: 2016/8/2 11:53
 */
public class HelpService extends AccessibilityService {
    private static final String       TEXT_FORCE_STOP = "强行停止";
    private static final String       TEXT_DETERMINE  = "确定";
    private static final CharSequence PACKAGE         = "com.android.settings";
    private static final CharSequence CLASS_NAME      = "android.widget.Button";

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN) @Override public void onAccessibilityEvent(final AccessibilityEvent event) {
        if(null == event || null == event.getSource()) { return; }
        if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            && event.getPackageName().equals(PACKAGE)
                /*&& event.getClassName().equals(CLASS_NAME)*/ ){

            simulationClick(event, TEXT_FORCE_STOP);
            simulationClick(event, TEXT_DETERMINE);
            performGlobalAction(GLOBAL_ACTION_BACK);
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

    @Override public void onInterrupt() { }
}
