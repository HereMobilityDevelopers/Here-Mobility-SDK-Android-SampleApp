package com.here.mobility.sdk.sampleapp;

import android.support.multidex.MultiDexApplication;

import com.here.mobility.sdk.core.HereMobilitySdk;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class SampleApplication extends MultiDexApplication {


    @Override
    public void onCreate() {
        super.onCreate();

        //HereMobilitySdk initialization must be called in your app's onCreate() method to initialize the SDK.
        HereMobilitySdk.init(this);

        //Returns whether the current process is the SDK agent process.
        if (HereMobilitySdk.isHereAgentProcess(this)){
            return;
        }

        // Put the rest of your app's initialization here
    }
}