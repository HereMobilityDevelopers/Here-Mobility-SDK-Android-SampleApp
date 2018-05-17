package com.here.mobility.sdk.sampleapp.util;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;
import java.net.URL;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class Utils {


    /**
     * Load image from url
     * @param url the image url
     * @return the drawable if success otherwise null.
     */
    @Nullable
    public static Drawable loadImageFromURL(@NonNull String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(is, null);
        } catch (Exception e) {
            return null;
        }
    }
    
}
