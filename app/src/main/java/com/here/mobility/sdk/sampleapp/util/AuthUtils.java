package com.here.mobility.sdk.sampleapp.util;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.here.mobility.sdk.common.util.AppBugException;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/

public class AuthUtils {

    private static final String LOG_TAG = AuthUtils.class.getSimpleName();


    /**
     * Generates HASH from User credentials.
     *
     * @param appKey The app key
     * @param userId The user identifier
     * @param expiration The expiration of token in seconds
     * @param key the app secret
     * @return The hashed string
     */
    @NonNull
    public static String hashHmac(@NonNull String appKey, @NonNull String userId, long expiration, @NonNull String key) {

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            Charset ascii = Charset.forName("US-ASCII");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(ascii), "HmacSHA256");
            sha256_HMAC.init(secretKey);
            String appKey64 = Base64.encodeToString(appKey.getBytes(ascii), Base64.NO_WRAP);
            String userId64 = Base64.encodeToString(userId.getBytes(ascii), Base64.NO_WRAP);

            String data = appKey64 + "." + userId64 + "." + Long.toString(expiration);

            byte[] rawHash = sha256_HMAC.doFinal(data.getBytes(ascii));
            StringBuilder hash = new StringBuilder();
            for (byte b: rawHash) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.wtf(LOG_TAG, "Failed generating HASH", e);
            throw new AppBugException("Failed generating HASH", e);
        }
    }
}