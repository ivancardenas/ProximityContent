package com.estimote.proximitycontent;

import android.app.Application;

import com.estimote.sdk.EstimoteSDK;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EstimoteSDK.initialize(getApplicationContext(), "proximitycontent-gx0", "0ab687894cd7cf8945728b235e109049");
    }
}
