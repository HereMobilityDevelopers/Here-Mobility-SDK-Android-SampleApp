package com.here.mobility.sdk.sampleapp.util;

import androidx.annotation.NonNull;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.here.mobility.sdk.core.ApplicationAuthInfo;
import com.here.mobility.sdk.core.MobilitySdk;
import com.here.mobility.sdk.core.net.ResponseFuture;
import com.here.mobility.sdk.sampleapp.R;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/

public class AuthUtils {

    private static final String LOG_TAG = AuthUtils.class.getSimpleName();


    /**
     * Generates HASH from Application credentials.
     *
     * @param appKey The app key
     * @param currentTimeSec The creation tome of the token in seconds since epoch
     * @param secretKey The app secret
     * @return The hashed string
     */
    @NonNull
    public static String hashHmac(@NonNull String appKey,
                                  long currentTimeSec,
                                  @NonNull String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA384");
            Charset ascii = Charset.forName("US-ASCII");
            mac.init(new SecretKeySpec(secretKey.getBytes(ascii), mac.getAlgorithm()));

            String apiKey64 = Base64.encodeToString(appKey.getBytes(ascii), Base64.NO_WRAP);
            String data = apiKey64 + "." + currentTimeSec;
            byte[] rawHash = mac.doFinal(data.getBytes(ascii));

            // Encode into a hexadecimal string
            StringBuilder hash = new StringBuilder();
            for (byte rawHashByte : rawHash) {
                hash.append(String.format("%02x", rawHashByte));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.wtf(LOG_TAG, "Failed generating HASH", e);
            throw new IllegalStateException("HmacSHA384 and US-ASCII must be supported", e);
        }
    }


    /**
     * App login for the HERE SDK.
     */
    @NonNull
    public static ResponseFuture<Void> appLogin(@NonNull Context context){
        return appLogin(context.getString(R.string.here_sdk_app_id),
                        context.getString(R.string.here_sdk_app_secret));
    }


    /**
     * App login for the HERE Demand SDK.
     */
    @NonNull
    public static ResponseFuture<Void> appLogin(@NonNull String appID, @NonNull String appSecret){

        //creation time time in second.
        long timeCreationInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

        //create hash code.
        String hash = AuthUtils.hashHmac(
                appID,
                timeCreationInSeconds, //timeCreationInSeconds The creation of the hash, in seconds since epoch
                appSecret);

        //create user info.
        ApplicationAuthInfo appInfo = ApplicationAuthInfo.create(
                hash,
                timeCreationInSeconds);

        //set the user info.
        return MobilitySdk.getInstance().authenticateApplication(appInfo);
    }
}