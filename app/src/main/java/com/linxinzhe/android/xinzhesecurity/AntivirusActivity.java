package com.linxinzhe.android.xinzhesecurity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.linxinzhe.android.xinzhesecurity.db.Md5AntivirusDao;
import com.linxinzhe.android.xinzhesecurity.utils.MD5Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AntivirusActivity extends ActionBarActivity {

    private static final String TAG = "AntivirusActivity";
    private static final int SCANNINGAPPINFO = 0;
    private static int appCount;
    private int appTotalNum;

    private PackageManager pm;

    private TextView mScanProgressTV;
    private TextView mScanningTV;

    private LinearLayout mScanningListLL;

    private List<AppInfo> viruses = new ArrayList<>();
    private Button mKillVirusBTN;
    private boolean virusFlag = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SCANNINGAPPINFO:
                    AppInfo appInfo = (AppInfo) (msg.obj);
                    mScanningTV.setText("正在扫描：" + appInfo.appName);
                    if (appInfo.isVirus) {
                        TextView tvV = new TextView(AntivirusActivity.this);
                        tvV.setText("病毒：" + appInfo.appPackageName);
                        tvV.setTextColor(Color.RED);
                        mScanningListLL.addView(tvV, 0);
                        virusFlag = true;
                    } else {
                        TextView tvC = new TextView(AntivirusActivity.this);
                        tvC.setText("安全：" + appInfo.appPackageName);
                        tvC.setTextColor(Color.GREEN);
                        mScanningListLL.addView(tvC, 0);
                    }
                    if (appCount != appTotalNum) {
                        mScanProgressTV.setText((int) ((++appCount) * 1.0 / appTotalNum * 100) + "%");
                        if (virusFlag) {
                            mKillVirusBTN.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_antivirus);
        mScanProgressTV = (TextView) findViewById(R.id.tv_scan_progress);
        mScanProgressTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mScanProgressTV.setClickable(false);
                appCount = 0;
                scan();
            }
        });
        mScanningTV = (TextView) findViewById(R.id.tv_scanning);
        mScanningListLL = (LinearLayout) findViewById(R.id.ll_scanning_list);

        //是病毒则提供卸载功能
        mKillVirusBTN = (Button) findViewById(R.id.btn_kill_virus);
        mKillVirusBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (AppInfo virus : viruses) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setAction("android.intent.action.DELETE");
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse("package:" + virus.appPackageName));
                    startActivityForResult(intent, 0);
                }
            }
        });
    }


    class AppInfo {
        String appName;
        String appPackageName;
        boolean isVirus;
    }

    private void scan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                pm = getPackageManager();
                List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
                String sourceDir = null;
                File file = null;
                appTotalNum = packageInfos.size();
                AppInfo appInfo = null;
                for (PackageInfo info : packageInfos) {
                    sourceDir = info.applicationInfo.sourceDir;
                    file = new File(sourceDir);

                    //获取文件的MD5值
                    String md5File = MD5Tools.encrypt(file);

                    Log.i(TAG, sourceDir + ":" + md5File);
                    appInfo = new AppInfo();
                    appInfo.appName = info.applicationInfo.loadLabel(pm).toString();
                    appInfo.appPackageName = info.packageName;

                    //对比MD5值判断是否病毒
                    if (Md5AntivirusDao.isVirus(AntivirusActivity.this, md5File)) {
                        appInfo.isVirus = true;
                        viruses.add(appInfo);
                    } else {
                        appInfo.isVirus = false;
                    }
                    Message msg = new Message();
                    msg.what = SCANNINGAPPINFO;
                    msg.obj = appInfo;
                    mHandler.sendMessage(msg);
                }
                mScanProgressTV.setClickable(true);
            }
        }).start();
    }
}
