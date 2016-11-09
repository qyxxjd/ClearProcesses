package com.classic.clearprocesses;

import android.content.Context;
import com.classic.adapter.BaseAdapterHelper;
import com.classic.adapter.CommonRecyclerAdapter;
import java.util.List;
import rx.functions.Action1;

/**
 * 应用名称: ClearProcesses
 * 包 名 称: com.classic.clearprocesses
 *
 * 文件描述: TODO
 * 创 建 人: 续写经典
 * 创建时间: 2016/7/25 18:05
 */
public class PackageAdapter extends CommonRecyclerAdapter<String> implements Action1<List<String>> {

    public PackageAdapter(Context context) {
        super(context, R.layout.item_app_info);
    }

    @Override public void onUpdate(BaseAdapterHelper helper, String item, int position) {
        helper.setText(R.id.item_pkg, item);
    }

    @Override public void call(List<String> items) {
        if(null != items && items.size() > 0){
            replaceAll(items);
        } else {
            clear();
        }
    }
}
