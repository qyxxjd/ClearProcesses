package com.classic.clearprocesses;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.classic.adapter.CommonRecyclerAdapter;
import com.tbruyelle.rxpermissions.RxPermissions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {

                if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                    query();
                    return;
                }

                RxPermissions.getInstance(mAppContext)
                             .request(Manifest.permission.KILL_BACKGROUND_PROCESSES)
                             .subscribe(new Action1<Boolean>() {
                                 @Override public void call(Boolean granted) {
                                     if (!granted) {
                                         return;
                                     }
                                     query();
                                 }
                             });
            }
        });

    }

    private void query(){
        mBackgroundProcesses = new ArrayList<>();
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
            Log.d("RunningAppProcessInfos", runningAppProcessInfos.toString());
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
            Log.d("RunningServiceInfos", runningServiceInfos.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return runningServiceInfos;
    }

    private boolean verifyPackage(String process) {
        if (process.startsWith("com.android") ||
                process.startsWith("com.google") ||
                process.startsWith("android") ||
                process.startsWith("system") ||
                process.startsWith("com.cyanogenmod") ||
                process.startsWith("org.cyanogenmod") ||
                process.startsWith("com.lbe") ||
                process.startsWith("com.classic")) {
            return true;
        }
        return false;
    }

    private void killAllProcesses(){
        addSubscription(
                Observable.create(new Observable.OnSubscribe<Long>() {
                    @Override public void call(Subscriber<? super Long> subscriber) {
                        for (Parcelable item : mBackgroundProcesses){
                            killProcess(item);
                        }
                    }
                }).subscribeOn(Schedulers.newThread())
                          .unsubscribeOn(Schedulers.newThread())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(new Action1<Long>() {
                              @Override public void call(Long aLong) {
                                  Toast.makeText(mAppContext, "共清理进程"+mBackgroundProcesses.size()+"个", Toast
                                          .LENGTH_SHORT).show();
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
            killAllProcesses();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void killProcess(Parcelable item){
        int pid = 0;
        int uid = 0;
        String packageName = "";
        if(item instanceof ActivityManager.RunningAppProcessInfo){
            pid = ((ActivityManager.RunningAppProcessInfo)item).pid;
            uid = ((ActivityManager.RunningAppProcessInfo)item).uid;
            packageName = ((ActivityManager.RunningAppProcessInfo)item).processName;
        }else if(item instanceof ActivityManager.RunningServiceInfo){
            pid = ((ActivityManager.RunningServiceInfo)item).pid;
            uid = ((ActivityManager.RunningServiceInfo)item).uid;
            packageName = ((ActivityManager.RunningServiceInfo)item).process;
        }
        //boolean isRoot = ShellUtil.checkRootPermission();
        if(packageName.indexOf(":") == -1){
            //ShellUtil.execCommand("adb shell", false);
            String cmd = "adb shell am force-stop "+packageName;
            try {
                execCommand(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //ShellUtil.CommandResult result = ShellUtil.execCommand(cmd, false);
            //android.os.Process.killProcess(pid);
            //Log.d("shell", cmd+","+result.toString());
        }
    }

    public void execCommand(String command) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);
        try {
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));
            StringBuffer stringBuffer = new StringBuffer();
            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line+"-");
            }
            System.out.println(stringBuffer.toString());

        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    @Override public void onItemClick(RecyclerView.ViewHolder viewHolder, View view, int position) {
        final Parcelable item = mBackgroundProcesses.get(position);

        if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            killProcess(item);
            return;
        }
        RxPermissions.getInstance(mAppContext)
                     .request(Manifest.permission.KILL_BACKGROUND_PROCESSES)
                     .subscribe(new Action1<Boolean>() {
                         @Override public void call(Boolean granted) {
                             if (!granted) {
                                 return;
                             }
                             killProcess(item);
                         }
                     });
    }
}
