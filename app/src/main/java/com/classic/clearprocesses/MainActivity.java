package com.classic.clearprocesses;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.classic.adapter.CommonRecyclerAdapter;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity implements CommonRecyclerAdapter.OnItemClickListener {
    private Context          mAppContext;
    private ActivityManager  mActivityManager;
    private List<Parcelable> mBackgroundProcesses;
    private ProcessAdapter   mProcessAdapter;
    private RecyclerView     mRecyclerView;
    private CompositeSubscription mCompositeSubscription;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAppContext = getApplicationContext();
        mBackgroundProcesses = new ArrayList<>();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRecyclerView = (RecyclerView) findViewById(R.id.main_rv);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mRecyclerView.getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mActivityManager = (ActivityManager) mAppContext.getSystemService(Context.ACTIVITY_SERVICE);
        mProcessAdapter = new ProcessAdapter(mAppContext);
        mRecyclerView.setAdapter(mProcessAdapter);
        mProcessAdapter.setOnItemClickListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                query();
            }
        });

    }

    @Override protected void onResume() {
        super.onResume();
        query();
    }

    private void query(){
        addSubscription(
                Observable.create(new Observable.OnSubscribe<List<Parcelable>>() {
                    @Override public void call(Subscriber<? super List<Parcelable>> subscriber) {
                        mBackgroundProcesses.clear();
                        mBackgroundProcesses.addAll(queryRunningAppProcesses());
                        mBackgroundProcesses.addAll(queryRunningServiceInfos());
                        subscriber.onNext(mBackgroundProcesses);
                    }
                }).subscribeOn(Schedulers.newThread())
                          .unsubscribeOn(Schedulers.newThread())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(mProcessAdapter)
        );
    }

    private List<ActivityManager.RunningAppProcessInfo> queryRunningAppProcesses() {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = new ArrayList<>();
        try {
            List<ActivityManager.RunningAppProcessInfo> infos = mActivityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : infos) {
                if (verifyPackage(info.processName)) continue;
                runningAppProcessInfos.add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return runningAppProcessInfos;
    }

    private List<ActivityManager.RunningServiceInfo> queryRunningServiceInfos() {
        List<ActivityManager.RunningServiceInfo>    runningServiceInfos = new ArrayList<>();
        try {
            List<ActivityManager.RunningServiceInfo> allServices = mActivityManager.getRunningServices(1000);
            for (ActivityManager.RunningServiceInfo serviceInfo : allServices) {
                if (verifyPackage(serviceInfo.process)) continue;
                runningServiceInfos.add(serviceInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return runningServiceInfos;
    }

    private boolean verifyPackage(String process) {
        if (process.startsWith("com.android") ||
                process.startsWith("com.eg.android") ||
                process.startsWith("com.google") ||
                process.startsWith("android") ||
                process.startsWith("system") ||
                process.startsWith("com.cyanogenmod") ||
                process.startsWith("org.cyanogenmod") ||
                process.startsWith("com.classic.clearprocesses") ||
                process.startsWith("com.qualcomm") || //高通cpu监控进程
                process.startsWith("com.huawei")
                ) {
            return true;
        }
        return false;
    }

    private void showAllPackage(){
        addSubscription(
                Observable.create(new Observable.OnSubscribe<Long>() {
                    @Override public void call(Subscriber<? super Long> subscriber) {
                        for (Parcelable item : mBackgroundProcesses){
                            showPackageDetail(item);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).subscribeOn(Schedulers.newThread())
                          .unsubscribeOn(Schedulers.newThread())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(new Action1<Long>() {
                              @Override public void call(Long aLong) {

                              }
                          })
        );
    }

    @Override protected void onDestroy() {
        if (null != mCompositeSubscription) {
            mCompositeSubscription.unsubscribe();
        }
        super.onDestroy();
    }

    private void addSubscription(Subscription subscription) {
        if (null == mCompositeSubscription) {
            mCompositeSubscription = new CompositeSubscription();
        }
        mCompositeSubscription.add(subscription);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_kill) {
            showAllPackage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPackageDetail(Parcelable item){
        String packageName = "";
        if(item instanceof ActivityManager.RunningAppProcessInfo){
            packageName = ((ActivityManager.RunningAppProcessInfo)item).processName;
        }else if(item instanceof ActivityManager.RunningServiceInfo){
            packageName = ((ActivityManager.RunningServiceInfo)item).process;
        }
        if(packageName.contains(":")){
            packageName = packageName.split(":")[0];
        }
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override public void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position) {
        final Parcelable item = mBackgroundProcesses.get(position);
        showPackageDetail(item);
    }
}
