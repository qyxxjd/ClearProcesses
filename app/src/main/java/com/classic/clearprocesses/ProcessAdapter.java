package com.classic.clearprocesses;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Parcelable;
import android.text.TextUtils;
import com.classic.adapter.BaseAdapterHelper;
import com.classic.adapter.CommonRecyclerAdapter;
import java.util.List;
import rx.functions.Action1;

/**
 * 应用名称: ClearProcesses
 * 包 名 称: com.classic.clearprocesses
 *
 * 文件描述: TODO
 * 创 建 人: 刘宾
 * 创建时间: 2016/7/25 16:05
 */
public class ProcessAdapter extends CommonRecyclerAdapter<Parcelable> implements Action1<List<Parcelable>> {

    public ProcessAdapter(Context context) {
        super(context, R.layout.item_app_info);
    }

    @Override public void onUpdate(BaseAdapterHelper helper, Parcelable item, int position) {
        String process = "";
        String pid = "";
        String uid = "";
        String pkgList = "";
        String type = "";
        if (item instanceof ActivityManager.RunningAppProcessInfo) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) item;
            process = "process:" + info.processName;
            pid = "pid:" + info.pid;
            uid = "uid:" + info.uid;
            pkgList = info.pkgList.toString();
            type = "type:RunningAppProcessInfo";
        } else if (item instanceof ActivityManager.RunningServiceInfo) {
            ActivityManager.RunningServiceInfo info = (ActivityManager.RunningServiceInfo) item;
            process = "process:" + info.process;
            pid = "pid:" + info.pid;
            uid = "uid:" + info.uid;
            type = "type:RunningServiceInfo";
        }
        helper.setText(R.id.item_process, process)
              .setText(R.id.item_type, type)
              .setText(R.id.item_pid, pid)
              .setText(R.id.item_uid, uid)
              .setText(R.id.item_pkg_list, pkgList)
              .setVisible(R.id.item_pkg_list, !TextUtils.isEmpty(pkgList));
    }

    @Override public void call(List<Parcelable> parcelables) {
        addAll(parcelables);
    }
}
