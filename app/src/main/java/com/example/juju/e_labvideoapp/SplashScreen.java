package com.example.juju.e_labvideoapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;


public class SplashScreen extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        XXPermissions.with(this)
                // 可设置被拒绝后继续申请，直到用户授权或者永久拒绝
                //.constantRequest()
                // 支持请求6.0悬浮窗权限8.0请求安装权限
                //.permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES)
                // 不指定权限则自动获取清单中的危险权限
                .permission(Permission.CAMERA, Permission.RECORD_AUDIO, Permission.ACCESS_FINE_LOCATION, Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_EXTERNAL_STORAGE)
                .request(new OnPermission() {

                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {
                        new Handler().postDelayed(new Runnable() {

                            /*
                             * Showing splash screen with a timer. This will be useful when you
                             * want to show case your app logo / company
                             */

                            @Override
                            public void run() {
                                // This method will be executed once the timer is over
                                // Start your app main activity
                                Intent i = new Intent(SplashScreen.this, MainActivity.class);
                                startActivity(i);

                                // close this activity
                                finish();
                            }
                        }, SPLASH_TIME_OUT);
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {

                    }
                });
    }


}