package com.here.mobility.sdk.sampleapp;

import android.app.Application;
import android.support.annotation.NonNull;


import com.here.mobility.sdk.core.HereMobilitySdk;
import com.here.mobility.sdk.core.auth.HereSdkUserAuthInfo;
import com.here.mobility.sdk.sampleapp.util.AuthUtils;
import com.here.mobility.sdk.sampleapp.util.Constant;

import java.util.concurrent.TimeUnit;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class SampleApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        //HereMobilitySdk initialization must be called in onCreate() method to initialize the SDK.
        HereMobilitySdk.init(this);

        //Returns whether the current process is the SDK agent process.
        if (HereMobilitySdk.isHereAgentProcess(this)){
            return;
        }

        //Check if user is stored in the SDK. Demand requests must be with registered user.
        if (HereMobilitySdk.getUserAuthInfo() == null){

            //the user registration should be done with your app's backend (see the documentation for more info).
            //This is a snippet to generate the token in the app, for testing purposes.
            registerUser("userName");
        }

        //put here the rest of you app sdks initialization.
    }


    /**
     * Register user to sdk.
     * @param userID the user name.
     */
    private void registerUser(@NonNull String userID){

        //expiration time in second.
        long timeExpirationInSeconds = TimeUnit.MILLISECONDS.
                toSeconds(System.currentTimeMillis()) + TimeUnit.HOURS.toSeconds(Constant.EXPIRATION_HOURS);

        //create hash code.
        String hash = AuthUtils.hashHmac(
                getString(R.string.here_sdk_app_id),
                userID, //newUserId the user ID
                (int)timeExpirationInSeconds, //newExpirationSec The expiration of the hash, in seconds since epoch
                getString(R.string.here_sdk_app_secret));

        //create user info.
        HereSdkUserAuthInfo userInfo = HereSdkUserAuthInfo.create(
                userID,
                (int) timeExpirationInSeconds,
                hash);

        //set the user info.
        HereMobilitySdk.setUserAuthInfo(userInfo);
    }
}