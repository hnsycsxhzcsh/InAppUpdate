package com.inappupdate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_INSTALL_PERMISSION = 10001;// 安装界面的请求码
    private static final int REQUEST_CODE_EXTERNAL_PERMISSION = 10002;
    private static final int REQUEST_CODE_INSTALL_PACKAGES = 10003;
    //读内存权限
    int REQUEST_CODE_ALL_EXTERNAL_STORAGE = 1001;
    String[] PERMISSIONS_ALL_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private String data = "{versionCode:\"2\", versionName:\"1.0.2\", versionSize:\"8511488 \", publishDate:\"2019-01-02\"}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //请求读写权限
        requestPremission();

        //这里模拟获取后台数据
        try {
            JSONObject versionObject = new JSONObject(data);
            int serverVerCode = versionObject.getInt("versionCode");
            int currentVerCode = getVerCode(MainActivity.this);
            //如果后台返回的版本>当前应用的版本，那么提示用户更新
            if (serverVerCode > currentVerCode) {
                doNewVersionUpdate();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void doNewVersionUpdate() {
        new AlertDialog(MainActivity.this).builder().
                setTitle("是否更新应用").setMsg("版本提醒").
                setPositiveButton("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //判断是否含有读写内存权限
                        if (hasPermission(MainActivity.this, "android.permission.READ_EXTERNAL_STORAGE",
                                "android.permission.WRITE_EXTERNAL_STORAGE")) {
                            downApkFileAdapt8();
                        } else {
                            //假如用户之前已经设置了不提醒权限申请，那么直接申请权限不会有提示，那么必须弹出权限设置页面，让用户设置
                            exteranlPermissionActivitySetting(MainActivity.this);
                        }
                    }
                }).setNegativeButton("Cancel", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        }).show();
    }

    /**
     * 通过系统设置页面让用户手动设置
     *
     * @param context
     */
    private void exteranlPermissionActivitySetting(Context context) {
        Toast.makeText(MainActivity.this, "需手动设置读写权限", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        try {
            // 将用户引导到系统设置页面
            if (Build.VERSION.SDK_INT >= 9) {
//                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            } else if (Build.VERSION.SDK_INT <= 8) {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                intent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
            }
            startActivityForResult(intent, REQUEST_CODE_EXTERNAL_PERMISSION);
        } catch (Exception e) {
            //抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_EXTERNAL_PERMISSION);
        }
    }

    private void downApkFileAdapt8() {
        //适配8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean hasInstallPermission = getPackageManager().canRequestPackageInstalls();
            if (!hasInstallPermission) {
                //必须手动修改权限
                startInstallPermissionSettingActivity();
            } else {
                downApkFile();
            }
        } else {
            downApkFile();
        }
    }

    /**
     * 下载apk
     */
    protected void downApkFile() {
        // 检查sd卡的状态
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "sd 卡异常", Toast.LENGTH_SHORT).show();
            return;
        }
        //TODO 此处添加下载apk的代码,下载完成后调用install方法来进行安装
        Toast.makeText(MainActivity.this, "正在下载应用", Toast.LENGTH_SHORT).show();

    }

    /**
     * 适配8.0  跳转到设置-允许安装未知来源-页面
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        Toast.makeText(MainActivity.this, "需要手动设置，允许安装应用", Toast.LENGTH_SHORT).show();
        //注意这个是8.0新API
        Uri packageURI = Uri.parse("package:" + getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        startActivityForResult(intent, REQUEST_CODE_INSTALL_PACKAGES);
    }

    /**
     * 获取当前app的版本
     *
     * @param context
     * @return
     */
    public int getVerCode(Context context) {
        int verCode = -1;
        try {
            verCode = context.getPackageManager().getPackageInfo("com.inappupdate", 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("getVerCode", e.getMessage());
        }
        return verCode;
    }

    /**
     * 动态请求权限
     */
    private void requestPremission() {
        if (!hasPermission(this, PERMISSIONS_ALL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_ALL_STORAGE,
                    REQUEST_CODE_ALL_EXTERNAL_STORAGE);
        }
    }

    //判断是否有对应权限
    public static boolean hasPermission(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.
                    PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 安装apk文件
     *
     * @param apkFile
     */
    protected void install(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory("android.intent.category.DEFAULT");
        //适配7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.inappupdate.fileprovider", apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            //适配8.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean hasInstallPermission = getPackageManager().canRequestPackageInstalls();
                if (!hasInstallPermission) {
                    startInstallPermissionSettingActivity();
                    return;
                }
            }
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        MainActivity.this.startActivityForResult(intent, REQUEST_CODE_INSTALL_PERMISSION);
        //华为p20等有些机型，加上这句会报解析包时候出现错误，但是去掉后有些机型安装完不能自动拉起应用，现在去掉
        //可以用获取手机厂商和型号，来做筛选,例如：p20的话不执行以下句子，不是p20执行以下句子，但是现在还不知道哪些类似p20需要做筛选
//        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_INSTALL_PACKAGES) {
                //用户允许8.0安装权限后
                downApkFile();
            }
        }
        // 设置读取内存权限时候resultCode的返回值为0，所以在此来判断，权限是否已经设置好
        if (requestCode == REQUEST_CODE_EXTERNAL_PERMISSION) {
            if (hasPermission(MainActivity.this, "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE")) {
                //如果权限已经申请好，那么下载
                downApkFileAdapt8();
            }
        }
    }
}
